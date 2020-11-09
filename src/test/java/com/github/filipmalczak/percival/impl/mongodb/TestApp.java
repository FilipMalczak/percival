package com.github.filipmalczak.percival.impl.mongodb;

import com.github.filipmalczak.percival.Percival;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClients;
import com.mongodb.client.internal.MongoClientImpl;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication
@Percival
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

}
