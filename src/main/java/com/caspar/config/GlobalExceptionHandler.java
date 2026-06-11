package com.caspar.config;

import com.caspar.agent.exception.AgentException;
import com.caspar.agent.exception.LlmCallException;
import com.caspar.common.Result;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * 全局异常处理器
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理 Agent 业务异常
     */
    @ExceptionHandler(AgentException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<Void> handleAgentException(AgentException e) {
        logger.error("Agent exception: {}", e.getMessage());
        return Result.error("智能助手暂时无法处理，请稍后重试或手动操作");
    }

    /**
     * 处理大模型调用异常
     */
    @ExceptionHandler(LlmCallException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<Void> handleLlmCallException(LlmCallException e) {
        logger.error("LLM call failed: {}", e.getMessage());
        return Result.error("大模型服务暂时不可用，已为您跳转到手动操作页面");
    }

    /**
     * 处理参数异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleIllegalArgumentException(IllegalArgumentException e) {
        return Result.badRequest(e.getMessage());
    }

    /**
     * 处理 @Valid 请求体校验异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fieldError -> fieldError.getDefaultMessage() != null
                        ? fieldError.getDefaultMessage()
                        : "请求参数不合法")
                .orElse("请求参数不合法");
        return Result.badRequest(message);
    }

    /**
     * 处理 query/path 参数校验异常
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleConstraintViolationException(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .findFirst()
                .map(violation -> violation.getMessage() == null ? "请求参数不合法" : violation.getMessage())
                .orElse("请求参数不合法");
        return Result.badRequest(message);
    }

    /**
     * 处理绑定异常（如表单参数）
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleBindException(BindException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fieldError -> fieldError.getDefaultMessage() != null
                        ? fieldError.getDefaultMessage()
                        : "请求参数不合法")
                .orElse("请求参数不合法");
        return Result.badRequest(message);
    }

    /**
     * 处理访问拒绝异常
     */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result<Void> handleAccessDeniedException(AccessDeniedException e) {
        return Result.forbidden();
    }

    /**
     * 处理文件上传大小超限异常
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        logger.error("文件上传大小超限: ", e);
        return Result.badRequest("文件大小超过限制");
    }

    /**
     * 处理静态资源不存在异常
     */
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result<Void> handleNoResourceFoundException(NoResourceFoundException e) {
        return Result.notFound("资源不存在");
    }

    /**
     * 处理其他异常
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception e) {
        logger.error("服务器内部错误: ", e);
        return Result.error("服务器内部错误，请稍后重试");
    }
}
