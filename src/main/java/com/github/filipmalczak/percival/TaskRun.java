package com.github.filipmalczak.percival;

import lombok.SneakyThrows;
import org.bson.types.ObjectId;

import java.util.concurrent.Future;

public interface TaskRun<T> {
    ObjectId getRunId();
    ObjectId getTaskId();

    RunStatus getStatus();

    Future<T> asFuture();

    @SneakyThrows
    default T get(){
        return asFuture().get();
    }

    @SneakyThrows
    default void join(){
        asFuture().get();
    }
}
