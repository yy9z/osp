package com.caspar.controller;

import com.caspar.common.Result;
import com.caspar.util.AliyunOSSOperator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * 文件上传控制器
 * 提供统一的文件上传接口，将文件存储到阿里云OSS
 */
@RestController
@RequestMapping("/api/file")
@Tag(name = "文件上传", description = "通用文件上传接口")
public class FileController {

    private static final Logger logger = LoggerFactory.getLogger(FileController.class);
    private static final Path LOCAL_UPLOAD_DIR = Paths.get("campus-frontend", "public", "images", "uploads");
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp",
            ".pdf", ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx", ".txt"
    );
    private static final Set<String> ZIP_BASED_EXTENSIONS = Set.of(".docx", ".xlsx", ".pptx");

    @Autowired
    private AliyunOSSOperator aliyunOSSOperator;

    @Value("${app.file.max-size-bytes:10485760}")
    private long maxFileSizeBytes;

    /**
     * 单文件上传
     * 上传图片或其他文件到阿里云OSS，返回文件的访问URL
     *
     * @param file 上传的文件，支持图片、文档等常见格式
     * @return 文件的OSS访问URL
     */
    @PostMapping("/upload")
    @Operation(summary = "文件上传", description = "上传文件到阿里云OSS，返回文件访问URL")
    public Result<Map<String, String>> upload(
            @Parameter(description = "上传的文件") @RequestParam("file") MultipartFile file,
            @RequestParam(value = "mode", required = false) String mode) {

        // 参数校验
        if (file == null || file.isEmpty()) {
            return Result.badRequest("文件不能为空");
        }

        // 获取原始文件名
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            return Result.badRequest("文件名无效");
        }
        String safeFilename = Paths.get(originalFilename).getFileName().toString();
        String extension = extractExtension(safeFilename);
        if (extension == null || !ALLOWED_EXTENSIONS.contains(extension)) {
            return Result.badRequest("不支持的文件类型");
        }

        // 检查文件大小 (最大10MB)
        if (file.getSize() > maxFileSizeBytes) {
            return Result.badRequest("文件大小不能超过" + readableSize(maxFileSizeBytes));
        }

        // 检查 MIME 与文件签名（防双扩展名与伪造 Content-Type）
        String validationError;
        try {
            validationError = validateMimeAndSignature(file, extension);
        } catch (IOException e) {
            logger.error("读取上传文件失败", e);
            return Result.error("文件读取失败");
        }
        if (validationError != null) {
            return Result.badRequest(validationError);
        }

        // 本地开发模式：前端可显式指定 mode=local，避免依赖外网 OSS
        if ("local".equalsIgnoreCase(mode)) {
            try {
                String localUrl = saveToLocal(file, extension);
                Map<String, String> data = new HashMap<>();
                data.put("url", localUrl);
                data.put("filename", safeFilename);
                return Result.success("文件上传成功（本地模式）", data);
            } catch (IOException e) {
                logger.error("Local upload failed: ", e);
                return Result.error("本地上传失败");
            }
        }

        try {
            // 上传到OSS
            String fileUrl = aliyunOSSOperator.upload(file);

            Map<String, String> data = new HashMap<>();
            data.put("url", fileUrl);
            data.put("filename", safeFilename);

            logger.info("File uploaded successfully: {}", fileUrl);
            return Result.success("文件上传成功", data);

        } catch (AliyunOSSOperator.ClientException e) {
            // OSS 异常时自动降级到本地上传，确保演示/开发可用
            logger.warn("OSS upload failed, fallback to local upload: {}", e.getMessage());
            try {
                String localUrl = saveToLocal(file, extension);
                Map<String, String> data = new HashMap<>();
                data.put("url", localUrl);
                data.put("filename", safeFilename);
                return Result.success("文件上传成功（本地兜底）", data);
            } catch (IOException ioException) {
                logger.error("Local fallback upload failed: ", ioException);
                return Result.error("文件上传失败");
            }
        } catch (Exception e) {
            logger.error("Upload error: ", e);
            return Result.error("文件上传失败");
        }
    }

    /**
     * 多文件上传
     * 批量上传多个文件到阿里云OSS
     *
     * @param files 上传的文件数组
     * @return 文件的OSS访问URL列表
     */
    @PostMapping("/upload/multiple")
    @Operation(summary = "多文件上传", description = "批量上传文件到阿里云OSS")
    public Result<Map<String, Object>> uploadMultiple(
            @Parameter(description = "上传的文件列表") @RequestParam("files") MultipartFile[] files) {

        if (files == null || files.length == 0) {
            return Result.badRequest("文件列表不能为空");
        }

        if (files.length > 10) {
            return Result.badRequest("一次最多上传10个文件");
        }

        Map<String, Object> resultData = new HashMap<>();
        int successCount = 0;
        int failCount = 0;

        for (MultipartFile file : files) {
            try {
                if (file == null || file.isEmpty()) {
                    failCount++;
                    continue;
                }
                String originalFilename = file.getOriginalFilename();
                String safeFilename = originalFilename == null ? "" : Paths.get(originalFilename).getFileName().toString();
                String extension = extractExtension(safeFilename);
                if (extension == null || !ALLOWED_EXTENSIONS.contains(extension)) {
                    logger.warn("Blocked upload file with invalid extension: {}", safeFilename);
                    failCount++;
                    continue;
                }
                if (file.getSize() > maxFileSizeBytes) {
                    logger.warn("Blocked upload file with oversize: {}, size={}", safeFilename, file.getSize());
                    failCount++;
                    continue;
                }
                String validationError = validateMimeAndSignature(file, extension);
                if (validationError != null) {
                    logger.warn("Blocked upload file {}: {}", safeFilename, validationError);
                    failCount++;
                    continue;
                }

                String fileUrl = aliyunOSSOperator.upload(file);
                resultData.put("url_" + successCount, fileUrl);
                successCount++;
            } catch (Exception e) {
                logger.error("Failed to upload file: {}", file.getOriginalFilename(), e);
                failCount++;
            }
        }

        resultData.put("successCount", successCount);
        resultData.put("failCount", failCount);

        if (failCount > 0) {
            return Result.success("部分文件上传成功", resultData);
        }
        return Result.success("所有文件上传成功", resultData);
    }

    private String saveToLocal(MultipartFile file, String extension) throws IOException {
        Files.createDirectories(LOCAL_UPLOAD_DIR);
        String filename = System.currentTimeMillis() + "_" + UUID.randomUUID().toString().replace("-", "") + extension;
        Path target = LOCAL_UPLOAD_DIR.resolve(filename);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        return "/images/uploads/" + filename;
    }

    private String extractExtension(String filename) {
        if (filename == null || filename.isBlank()) {
            return null;
        }
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return null;
        }
        return filename.substring(dot).toLowerCase(Locale.ROOT);
    }

    private String readableSize(long bytes) {
        if (bytes <= 0) {
            return "0B";
        }
        if (bytes % (1024L * 1024L) == 0) {
            return (bytes / (1024L * 1024L)) + "MB";
        }
        if (bytes % 1024L == 0) {
            return (bytes / 1024L) + "KB";
        }
        return bytes + "B";
    }

    private String validateMimeAndSignature(MultipartFile file, String extension) throws IOException {
        byte[] header = readHeaderBytes(file, 64);
        if (!hasValidSignature(extension, header)) {
            return "文件内容与扩展名不匹配";
        }

        String declaredMime = normalizeMime(file.getContentType());
        if (!isMimeCompatible(extension, declaredMime)) {
            return "文件 MIME 类型不合法";
        }

        String sniffedMime = normalizeMime(detectMimeType(header));
        if (!isMimeCompatible(extension, sniffedMime)) {
            return "文件内容类型校验失败";
        }
        return null;
    }

    private byte[] readHeaderBytes(MultipartFile file, int maxBytes) throws IOException {
        try (InputStream in = file.getInputStream()) {
            return in.readNBytes(Math.max(1, maxBytes));
        }
    }

    private String detectMimeType(byte[] header) {
        if (header == null || header.length == 0) {
            return null;
        }
        try (ByteArrayInputStream in = new ByteArrayInputStream(header)) {
            return URLConnection.guessContentTypeFromStream(in);
        } catch (IOException e) {
            return null;
        }
    }

    private String normalizeMime(String mimeType) {
        if (mimeType == null) {
            return null;
        }
        String normalized = mimeType.trim().toLowerCase(Locale.ROOT);
        int semicolonIndex = normalized.indexOf(';');
        return semicolonIndex > 0 ? normalized.substring(0, semicolonIndex) : normalized;
    }

    private boolean isMimeCompatible(String extension, String mimeType) {
        if (mimeType == null || mimeType.isBlank()) {
            return true;
        }
        if ("application/octet-stream".equals(mimeType) || "binary/octet-stream".equals(mimeType)) {
            return true;
        }
        if (ZIP_BASED_EXTENSIONS.contains(extension)
                && ("application/zip".equals(mimeType) || "application/x-zip-compressed".equals(mimeType))) {
            return true;
        }
        return switch (extension) {
            case ".jpg", ".jpeg" -> Set.of("image/jpeg", "image/jpg", "image/pjpeg").contains(mimeType);
            case ".png" -> Set.of("image/png").contains(mimeType);
            case ".gif" -> Set.of("image/gif").contains(mimeType);
            case ".bmp" -> Set.of("image/bmp", "image/x-ms-bmp").contains(mimeType);
            case ".webp" -> Set.of("image/webp").contains(mimeType);
            case ".pdf" -> Set.of("application/pdf", "application/x-pdf").contains(mimeType);
            case ".doc" -> Set.of("application/msword", "application/vnd.ms-word").contains(mimeType);
            case ".docx" -> Set.of("application/vnd.openxmlformats-officedocument.wordprocessingml.document").contains(mimeType);
            case ".xls" -> Set.of("application/vnd.ms-excel").contains(mimeType);
            case ".xlsx" -> Set.of("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet").contains(mimeType);
            case ".ppt" -> Set.of("application/vnd.ms-powerpoint").contains(mimeType);
            case ".pptx" -> Set.of("application/vnd.openxmlformats-officedocument.presentationml.presentation").contains(mimeType);
            case ".txt" -> Set.of("text/plain").contains(mimeType);
            default -> false;
        };
    }

    private boolean hasValidSignature(String extension, byte[] header) {
        return switch (extension) {
            case ".jpg", ".jpeg" -> startsWith(header, new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});
            case ".png" -> startsWith(header, new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A});
            case ".gif" -> startsWith(header, "GIF87a".getBytes()) || startsWith(header, "GIF89a".getBytes());
            case ".bmp" -> startsWith(header, new byte[]{0x42, 0x4D});
            case ".webp" -> header.length >= 12
                    && startsWith(header, "RIFF".getBytes())
                    && header[8] == 'W' && header[9] == 'E' && header[10] == 'B' && header[11] == 'P';
            case ".pdf" -> startsWith(header, new byte[]{0x25, 0x50, 0x44, 0x46});
            case ".doc", ".xls", ".ppt" -> startsWith(header, new byte[]{
                    (byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0, (byte) 0xA1, (byte) 0xB1, 0x1A, (byte) 0xE1
            });
            case ".docx", ".xlsx", ".pptx" -> startsWith(header, new byte[]{0x50, 0x4B, 0x03, 0x04})
                    || startsWith(header, new byte[]{0x50, 0x4B, 0x05, 0x06})
                    || startsWith(header, new byte[]{0x50, 0x4B, 0x07, 0x08});
            case ".txt" -> isLikelyText(header);
            default -> false;
        };
    }

    private boolean startsWith(byte[] source, byte[] prefix) {
        if (source == null || prefix == null || source.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (source[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }

    private boolean isLikelyText(byte[] data) {
        if (data == null || data.length == 0) {
            return true;
        }
        for (byte b : data) {
            int value = b & 0xFF;
            if (value == 0) {
                return false;
            }
            if (value < 0x09) {
                return false;
            }
            if (value > 0x0D && value < 0x20) {
                return false;
            }
        }
        return true;
    }
}
