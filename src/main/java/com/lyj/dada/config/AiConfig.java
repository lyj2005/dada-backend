package com.lyj.dada.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "ai.config")
@Data
public class AiConfig {

    private String apiKey;

    private String url;

    private String model;
}