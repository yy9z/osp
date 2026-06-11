package com.caspar.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI/Swagger配置类
 */
@Configuration
public class OpenApiConfig {

    /**
     * 配置OpenAPI信息
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("本地开发服务器")
                ))
                .info(new Info()
                        .title("高校校园一站式平台 API")
                        .version("1.0.0")
                        .description("校园导航模块API文档，提供校园POI查询、附近搜索、路径规划等功能")
                        .contact(new Contact()
                                .name("开发团队")
                                .email("support@campus.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")));
    }
}
