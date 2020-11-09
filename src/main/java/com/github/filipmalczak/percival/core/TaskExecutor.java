package com.github.filipmalczak.percival.core;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

public interface TaskExecutor {
    <P> Future<P> execute(Callable<P> task);

    //todo shutdownNow and friends?
    void shutdown();
}
