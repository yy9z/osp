package com.caspar.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AmapConfig {

    @Value("${amap.web.key}")
    private String amapWebKey;

    @Value("${amap.web.security.code:}")
    private String amapWebSecurityCode;

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    public String getAmapWebKey() {
        return amapWebKey;
    }

    public String getAmapWebSecurityCode() {
        return amapWebSecurityCode;
    }
}
