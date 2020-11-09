package com.github.filipmalczak.percival.core;

import com.github.filipmalczak.percival.core.TaskExecutor;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;

import java.util.Map;
import java.util.concurrent.*;

public class TaskExecutors {
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class CurrentThreadTaskExecutor implements TaskExecutor {
        private static final CurrentThreadTaskExecutor INSTANCE = new CurrentThreadTaskExecutor();

        @Override
        @SneakyThrows
        public <P> Future<P> execute(Callable<P> task) {
            FutureTask<P> futureTask = new FutureTask<>(task);
            futureTask.run();
            return futureTask;
        }

        @Override
        public void shutdown() {

        }
    }

    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    public static class ExecutorServiceImpl implements TaskExecutor {
        ExecutorService service;

        @Override
        @SneakyThrows
        public <P> Future<P> execute(Callable<P> task) {
            return service.submit(task);
        }

        @Override
        public void shutdown() {
            service.shutdown();
        }
    }

    private static final TaskExecutor COMMON_SINGLE_THREAD = new ExecutorServiceImpl(Executors.newSingleThreadExecutor());
    private static final Map<Integer, TaskExecutor> DEDICATED_SINGLE_THREAD = new ConcurrentHashMap<>();


    public static TaskExecutor currentThread(){
        return CurrentThreadTaskExecutor.INSTANCE;
    }

    public static TaskExecutor here(){
        return currentThread();
    }

    public static TaskExecutor commonSingleThread(){
        return COMMON_SINGLE_THREAD;
    }

    public static TaskExecutor dedicatedSingleThread(int id){
        if (!DEDICATED_SINGLE_THREAD.containsKey(id))
            DEDICATED_SINGLE_THREAD.put(id, new ExecutorServiceImpl(Executors.newSingleThreadExecutor()));
        return DEDICATED_SINGLE_THREAD.get(id);
    }

    public static TaskExecutor inQueue(){
        return commonSingleThread();
    }

    public static TaskExecutor fixedPool(int size){
        return new ExecutorServiceImpl(Executors.newFixedThreadPool(size));
    }

    public static TaskExecutor cachedPool(){
        return new ExecutorServiceImpl(Executors.newCachedThreadPool());
    }
}
