package com.github.filipmalczak.percival.impl.mongodb;

import com.github.filipmalczak.percival.api.Task;
import com.github.filipmalczak.percival.core.TaskKey;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.stream.Stream;

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

    public void cleanup(){
        template.getDb().drop();
    }

    @Value(staticConstructor = "from")
    static class Labeled<T> {
        String label;
        T value;
    }

    @Value(staticConstructor = "of")
    static class Suffix {
        String suffix;

        public static Suffix none(){
            return Suffix.of("");
        }

        public String on(String txt){
            return txt+suffix;
        }

        int on(int base){
            return (int) (base+Math.sqrt(suffix.hashCode()));
        }
    }

    Stream<Labeled<TaskKey>> labeledKeys(){
        return labeledKeys(Suffix.none());
    }

    @Data
    @AllArgsConstructor
    static class IntString {
        int anInt;
        String string;
    }

    Stream<Labeled<TaskKey>> labeledKeys(Suffix suffix){
        return Stream.of(
            Labeled.from("just name", TaskKey.of(suffix.on("justname"))),
            Labeled.from("just int param", TaskKey.of(suffix.on(111))),
            Labeled.from("just int-string param",
                new TaskKey(
                    null,
                    new IntString(
                        suffix.on(123),
                        suffix.on("intstring")
                    )
                )
            ),
            Labeled.from("name and int param",
                new TaskKey(
                    suffix.on("nameandint"),
                    suffix.on(111)
                )
            ),
            Labeled.from("name and int-string param",
                new TaskKey(
                    suffix.on("nameandintstring"),
                    new IntString(
                        suffix.on(456),
                        suffix.on("intstringparam")
                    )
                )
            )
        );
    }

    @TestFactory
    Stream<DynamicTest> simpleShallowTree(){
        return labeledKeys(Suffix.of("root")).flatMap(r ->
            labeledKeys(Suffix.of("first")).flatMap(f ->
                labeledKeys(Suffix.of("second")).flatMap(s ->
                    Stream.of(
                        DynamicTest.dynamicTest(
                            "Trace of simple shallow tree ("+
                                "root: "+r.label+", " +
                                "first: "+f.label+", "+
                                "second: "+s.label+", "+
                                ") [mixed API]",
                            () -> {
                                cleanup();
                                log.info("r: "+r+", f: "+f+", s: "+s+"[mixed]");
                                simpleShallowTreeMixed(r.value, f.value, s.value);
                            }
                        ),
                        DynamicTest.dynamicTest(
                            "Trace of simple shallow tree ("+
                                "root: "+r.label+", " +
                                "first: "+f.label+", "+
                                "second: "+s.label+", "+
                                ") [all fluent API]",
                            () -> {
                                cleanup();
                                log.info("r: "+r+", f: "+f+", s: "+s+"[fluent]");
                                simpleShallowTreeAllFluent(r.value, f.value, s.value);
                            }
                        )
                    )

                )
            )
        );
    }

    public void simpleShallowTreeMixed(TaskKey<Object> root, TaskKey<Object> first, TaskKey<Object> second){
        List<Integer> trace = new ArrayList<>();

        Throwable e = null;
        try {
            tasks.task(root).running().recursive(t -> {
                t.createTask(first, (Object p, Task t2) -> {
                    trace.add(1);
                    return 0;
                }).join();
                t.toBeContinued();
            }).join();
        } catch (Exception exec){
            if (exec instanceof ExecutionException)
                e = exec.getCause();
            else
                e = exec;
        }
        log.info("E: "+e);
        assertNotNull(e);
        assertTrue(e instanceof Task.ToBeContinued);

        e = null;
        try{
            tasks.createTask(root, (Object params, Task t) -> {
                t.task(first).running().simple(() -> {
                    trace.add(2);
                }).join();
                t.task(second).running().simple(() -> {
                    trace.add(3);
                }).join();
                t.toBeContinued();
                return 1;
            }).join();

        } catch (Exception exec){
            if (exec instanceof ExecutionException)
                e = exec.getCause();
            else
                e = exec;
        }
        log.info("E: "+e);
        assertNotNull(e);
        assertTrue(e instanceof Task.ToBeContinued);

        assertEquals(trace, asList(1, 3));
        log.info("Trace: "+trace);
    }

    public void simpleShallowTreeAllFluent(TaskKey<Object> root, TaskKey<Object> first, TaskKey<Object> second){
        List<Integer> trace = new ArrayList<>();

        Throwable e = null;
        try {
            tasks.task(root).running().recursive(t -> {
                t.task(first).running().simple(() -> {
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
        log.info("E: "+e);
        assertNotNull(e);
        assertTrue(e instanceof Task.ToBeContinued);

        e = null;
        try{
            tasks.task(root).running().recursive(t -> {
                t.task(first).running().simple(() -> {
                    trace.add(2);
                }).join();
                t.task(second).running().simple(() -> {
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
        log.info("E: "+e);
        assertNotNull(e);
        assertTrue(e instanceof Task.ToBeContinued);

        assertEquals(trace, asList(1, 3));
        log.info("Trace: "+trace);
    }
}