package com.example.bizvision.config;

import com.example.bizvision.common.mybatis.CodeEnumTypeHandler;
import org.mybatis.spring.boot.autoconfigure.ConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MyBatisConfig {
    @Bean
    public ConfigurationCustomizer codeEnumTypeHandlerCustomizer() {
        return configuration -> configuration.setDefaultEnumTypeHandler(CodeEnumTypeHandler.class);
    }
}
