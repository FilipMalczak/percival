package com.github.filipmalczak.percival.api;

import com.github.filipmalczak.percival.core.TaskExecutor;
import com.github.filipmalczak.percival.core.TaskKey;
import com.github.filipmalczak.percival.core.TaskRun;

import java.util.function.BiFunction;
import java.util.stream.Stream;

public interface Task extends FluentTaskAPI, FluentTaskExecutorAPI {
    TaskExecutor getCurrentExecutor();
    void setCurrentExecutor(TaskExecutor executor);

    <T, P> TaskRun<T> task(TaskKey<P> key, BiFunction<P, Task, T> body);

    Stream<TaskRun<?>> getAllRuns();

    default void joinAll(){
        getAllRuns().forEach(TaskRun::join);
    }

    default void toBeContinued(){
        joinAll();
        throw new ToBeContinued();
    }

    class ToBeContinued extends RuntimeException {}
}
