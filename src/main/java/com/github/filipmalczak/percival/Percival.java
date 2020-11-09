package com.github.filipmalczak.percival;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@EnableMongoRepositories//(basePackageClasses = Percival.class)
@Import(Percival.Config.class)
public @interface Percival {
    @ComponentScan(basePackageClasses = Percival.class)
    @Configuration
    static class Config {}
}
