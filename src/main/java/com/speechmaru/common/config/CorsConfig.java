package com.speechmaru.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Value("${spring.origin}")
    private String ORIGIN;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOrigins(ORIGIN)
            .allowedMethods("GET", "POST", "PUT", "DELETE")
            .allowedHeaders("Origin", "X-Requested-With", "Content-Type", "Accept")
            .allowCredentials(true);
    }
}
