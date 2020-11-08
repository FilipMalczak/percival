package com.github.filipmalczak.percival;

import lombok.SneakyThrows;

import java.util.concurrent.Callable;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public interface FluentTaskAPI {
    interface CalculatingClosure<P> {
        <T> TaskRun<T> subtask(BiFunction<P, TaskExecutor, T> body);

        <T> TaskRun<T> parametrized(Function<P, T> body);

        <T> TaskRun<T> recursive(Function<TaskExecutor, T> body);

        <T> TaskRun<T> simple(Callable<T> body);
    }

    interface RunningClosure<P> {
        TaskRun<Void> subtask(BiConsumer<P, TaskExecutor> body);

        TaskRun<Void> parametrized(Consumer<P> body);

        TaskRun<Void> recursive(Consumer<TaskExecutor> body);

        TaskRun<Void> simple(Runnable body);
    }

    interface BodyClosure<P> {
        CalculatingClosure<P> calculating();
        RunningClosure<P> running();
    }

    default <P> BodyClosure<P> task(TaskKey<P> key){
        return new BodyClosure<P>() {

            @Override
            public CalculatingClosure<P> calculating() {
                return new CalculatingClosure<P>() {
                    @Override
                    public <T> TaskRun<T> subtask(BiFunction<P, TaskExecutor, T> body) {
                        return taskImpl(key, body);
                    }

                    @Override
                    public <T> TaskRun<T> parametrized(Function<P, T> body) {
                        return subtask((p, e) -> body.apply(p));
                    }

                    @Override
                    public <T> TaskRun<T> recursive(Function<TaskExecutor, T> body) {
                        return subtask((p, e) -> body.apply(e));
                    }

                    @SneakyThrows
                    private <T> T safeCall(Callable<T> c){
                        return c.call();
                    }

                    @Override
                    public <T> TaskRun<T> simple(Callable<T> body) {
                        return subtask((p, e) -> safeCall(body));
                    }
                };
            }

            @Override
            public RunningClosure<P> running() {
                return new RunningClosure<P>() {
                    @Override
                    public TaskRun<Void> subtask(BiConsumer<P, TaskExecutor> body) {
                        return taskImpl(key, (p, e) -> {body.accept(p, e); return null;});
                    }

                    @Override
                    public TaskRun<Void> parametrized(Consumer<P> body) {
                        return subtask((p, e) -> body.accept(p));
                    }

                    @Override
                    public TaskRun<Void> recursive(Consumer<TaskExecutor> body) {
                        return subtask((p, e) -> body.accept(e));
                    }

                    @Override
                    public TaskRun<Void> simple(Runnable body) {
                        return subtask((p, e) -> body.run());
                    }
                };
            }
        };
    }

    default BodyClosure<Void> task(String name){
        return task(TaskKey.of(name));
    }

    default <P> BodyClosure<P> task(P parameters){
        return task(TaskKey.of(parameters));
    }

    default <P> BodyClosure<P> task(String name, P parameters){
        return task(new TaskKey(name, parameters));
    }

    default <T, P> TaskRun<T> taskImpl(TaskKey<P> key, BiFunction<P, TaskExecutor, T> body){
        return task(key, body);
    }

    <T, P> TaskRun<T> task(TaskKey<P> key, BiFunction<P, TaskExecutor, T> body);
}
