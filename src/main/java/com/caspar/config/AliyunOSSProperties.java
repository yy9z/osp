package com.caspar.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 阿里云OSS配置属性类
 * 用于绑定 application.properties 中的 aliyun.oss 相关配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "aliyun.oss")
public class AliyunOSSProperties {

    /**
     * OSS区域节点地址
     * 例如: oss-cn-hangzhou.aliyuncs.com
     */
    private String endpoint;

    /**
     * OSS存储空间名称
     */
    private String bucket;

    /**
     * OSS区域ID
     * 例如: cn-hangzhou
     */
    private String region;

    /**
     * AccessKey ID
     */
    private String accessKeyId;

    /**
     * AccessKey Secret
     */
    private String accessKeySecret;
}
