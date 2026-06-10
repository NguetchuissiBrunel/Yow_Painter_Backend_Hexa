package com.yowpainter.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({KernelProperties.class, KernelKafkaProperties.class})
public class KernelPropertiesConfig {
}
