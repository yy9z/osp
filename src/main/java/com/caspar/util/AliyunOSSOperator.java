package com.caspar.util;

import com.aliyun.oss.ClientBuilderConfiguration;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.common.auth.CredentialsProvider;
import com.aliyun.oss.common.auth.CredentialsProviderFactory;
import com.aliyun.oss.common.auth.DefaultCredentialProvider;
import com.aliyun.oss.common.auth.EnvironmentVariableCredentialsProvider;
import com.aliyun.oss.model.PutObjectRequest;
import com.caspar.config.AliyunOSSProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 阿里云OSS对象存储操作类
 * 负责将文件上传到阿里云OSS存储服务
 */
@Component
public class AliyunOSSOperator {

    private static final Logger logger = LoggerFactory.getLogger(AliyunOSSOperator.class);

    @Autowired
    private AliyunOSSProperties aliyunOSSProperties;

    /**
     * 上传文件到OSS
     *
     * @param content        文件字节数组
     * @param fileOriginName 文件原始名称
     * @return OSS上的文件访问URL
     * @throws ClientException 如果上传失败则抛出异常
     */
    public String upload(byte[] content, String fileOriginName) throws ClientException {
        String region = aliyunOSSProperties.getRegion();
        String endpoint = aliyunOSSProperties.getEndpoint();
        String bucketName = aliyunOSSProperties.getBucket();

        logger.info("OSS配置 - Region: {}, Endpoint: {}, Bucket: {}", region, endpoint, bucketName);

        // 生成文件存储路径: 年/月/UUID+原扩展名
        String dir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
        String uuidString = UUID.randomUUID().toString();
        String fileName = uuidString + fileOriginName.substring(fileOriginName.lastIndexOf("."));
        String objectName = dir + '/' + fileName;

        // 配置OSS凭证提供者
        CredentialsProvider credentialsProvider;
        String accessKeyId = aliyunOSSProperties.getAccessKeyId();
        String accessKeySecret = aliyunOSSProperties.getAccessKeySecret();

        if (accessKeyId != null && !accessKeyId.isEmpty() && accessKeySecret != null && !accessKeySecret.isEmpty()) {
            // 优先使用配置文件中的 AccessKey
            logger.info("Using AccessKey from configuration file.");
            credentialsProvider = new DefaultCredentialProvider(accessKeyId, accessKeySecret);
        } else {
            // 否则尝试从环境变量获取
            logger.info("Using AccessKey from environment variables.");
            try {
                credentialsProvider = CredentialsProviderFactory.newEnvironmentVariableCredentialsProvider();
            } catch (Exception e) {
                logger.error("Failed to initialize credentials provider: ", e);
                throw new ClientException("Failed to initialize credentials provider: " + e.getMessage());
            }
        }

        // 配置OSS客户端
        ClientBuilderConfiguration clientBuilderConfiguration = new ClientBuilderConfiguration();

        OSS ossClient = null;
        try {
            ossClient = OSSClientBuilder.create()
                    .endpoint(endpoint)
                    .credentialsProvider(credentialsProvider)
                    .clientConfiguration(clientBuilderConfiguration)
                    .region(region)
                    .build();

            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, objectName, new ByteArrayInputStream(content));
            ossClient.putObject(putObjectRequest);
            logger.info("File uploaded successfully: {}", objectName);
        } catch (OSSException oe) {
            logger.error("OSS Error - Code: {}, Message: {}", oe.getErrorCode(), oe.getErrorMessage(), oe);
            throw new ClientException("OSS upload failed: " + oe.getErrorMessage());
        } catch (Exception e) {
            logger.error("Upload error: ", e);
            throw new ClientException("Upload failed: " + e.getMessage());
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }

        // 构建返回的访问URL: https://bucket.endpoint/yyyy/MM/filename
        return endpoint.split("//")[0] + "//" + bucketName + '.' + endpoint.split("//")[1] + '/' + objectName;
    }

    /**
     * 上传MultipartFile到OSS
     *
     * @param file Spring上传的文件对象
     * @return OSS上的文件访问URL
     * @throws ClientException 如果上传失败则抛出异常
     * @throws IOException     如果读取文件失败则抛出异常
     */
    public String upload(MultipartFile file) throws ClientException, IOException {
        if (file == null || file.isEmpty()) {
            throw new ClientException("File is empty");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new ClientException("Original filename is invalid");
        }
        return upload(file.getBytes(), originalFilename);
    }

    /**
     * 自定义异常类
     */
    public static class ClientException extends Exception {
        public ClientException(String message) {
            super(message);
        }
    }
}
