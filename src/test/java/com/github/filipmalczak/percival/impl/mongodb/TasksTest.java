package com.github.filipmalczak.percival.impl.mongodb;

import com.github.filipmalczak.percival.api.Task;
import lombok.extern.slf4j.Slf4j;
import org.bson.BsonDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@Import(TestApp.class)
@Slf4j
class TasksTest {

    @Autowired
    Tasks tasks;

    @Autowired
    MongoTemplate template;

//    @BeforeEach
//    public void cleanup(){
//        for (String collection: template.getDb().listCollectionNames())
//            template.getCollection(collection).deleteMany(BsonDocument.parse("{}"));
//    }

    @Test
    public void simpleShallowTree(){
        List<Integer> trace = new ArrayList<>();

        Throwable e = null;
        try {
            tasks.task("root").running().recursive(t -> {
                t.task("first").running().simple(() -> {
                    trace.add(1);
                }).join();
                t.toBeContinued();
            }).join();
        } catch (Exception exec){
            if (exec instanceof ExecutionException)
                e = exec.getCause();
            else
                e = exec;
        }

        assertNotNull(e);
        assertTrue(e instanceof Task.ToBeContinued);

        e = null;
        try{
            tasks.task("root").running().recursive(t -> {
                t.task("first").running().simple(() -> {
                    trace.add(2);
                }).join();
                t.task("second").running().simple(() -> {
                    trace.add(3);
                }).join();
                t.toBeContinued();
            }).join();

        } catch (Exception exec){
            if (exec instanceof ExecutionException)
                e = exec.getCause();
            else
                e = exec;
        }

        assertNotNull(e);
        assertTrue(e instanceof Task.ToBeContinued);

        assertEquals(trace, asList(1, 3));
        log.info("Trace: "+trace);
    }
}