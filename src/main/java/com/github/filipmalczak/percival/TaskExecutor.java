package com.github.filipmalczak.percival;

import java.util.function.BiFunction;
import java.util.stream.Stream;

public interface TaskExecutor extends FluentTaskAPI {
    <T, P> TaskRun<T> task(TaskKey<P> key, BiFunction<P, TaskExecutor, T> body);

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
