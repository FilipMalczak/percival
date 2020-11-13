package com.github.filipmalczak.percival.impl.mongodb;

import com.github.filipmalczak.percival.PercivalConfig;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClients;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.Random;

@SpringBootApplication
@Import(PercivalConfig.class)
@Slf4j
public class TestApp {
    @Bean
    public MongoTemplate mongoTemplate(){
//        ConnectionString connectionString = new ConnectionString("mongodb://root:example@localhost:27017/test");
        ConnectionString connectionString = new ConnectionString("mongodb://localhost:27017/test");
        MongoClientSettings mongoClientSettings = MongoClientSettings.builder()
            .applyConnectionString(connectionString)
            .build();
        return new MongoTemplate(
            MongoClients.create(mongoClientSettings),
            "test"
        );
    }

    @SneakyThrows
    public static void safeSleep(long ms){
        Thread.sleep(ms);
    }

    public static void main(String[] args){
        log.info("START");
        int out = SpringApplication.run(TestApp.class).getBean(Tasks.class).task("root").calculating().recursive(t -> {
            log.info("Running root");
            int value = t.task("first").calculating().simple(() -> {
                log.info("First sleeping for 15s");
                safeSleep(15000);
                log.info("First sleep over");
                int val = new Random().nextInt(1000);
                log.info("First returning "+val);
                return val;
            }).get();
            log.info("Root value "+value);
            int another = t.task("second", 3).calculating().parametrized(i -> {
                log.info("Second sleeping for 10s");
                safeSleep(10000);
                log.info("Second sleep over");
                int val = 2 * i;
                log.info("Second returning "+val);
                return val;
            }).get();
            log.info("Root another "+another);
            int result = another + 5;
            log.info("Root returning "+result);
            return result;
        }).get();
        log.info("RESULT: "+out);
    }

}
