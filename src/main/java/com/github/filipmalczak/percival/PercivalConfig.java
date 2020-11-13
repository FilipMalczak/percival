package com.github.filipmalczak.percival;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@EnableMongoRepositories(basePackageClasses = PercivalConfig.class)
@ComponentScan(basePackageClasses = PercivalConfig.class)
@Configuration
public interface PercivalConfig {
}
