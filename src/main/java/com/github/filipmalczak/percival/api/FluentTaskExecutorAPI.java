package com.github.filipmalczak.percival.api;

import com.github.filipmalczak.percival.core.TaskExecutor;
import com.github.filipmalczak.percival.core.TaskExecutors;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

public interface FluentTaskExecutorAPI {
    void setCurrentExecutor(TaskExecutor executor);

    default ExecutorClosure executor(){
        return new ExecutorClosure(this);
    }

    //todo forkjoinpool

    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    final class ExecutorClosure {
        public FluentTaskExecutorAPI target;

        public void currentThread(){
            target.setCurrentExecutor(TaskExecutors.currentThread());
        }

        public void here(){
            currentThread();
        }

        public void commonSingleThread(){
            target.setCurrentExecutor(TaskExecutors.commonSingleThread());
        }

        public void dedicatedSingleThread(){
            dedicatedSingleThread(this);
        }

        public void dedicatedSingleThread(Object hashable){
            target.setCurrentExecutor(TaskExecutors.dedicatedSingleThread(hashable.hashCode()));
        }

        public void inQueue(Object hashable) {
            dedicatedSingleThread(hashable);
        }

        public void inQueue(){
            inQueue(this);
        }

        public void fixedPool(int size){
            target.setCurrentExecutor(TaskExecutors.fixedPool(size));
        }

        public void cachedPool(){
            target.setCurrentExecutor(TaskExecutors.cachedPool());
        }


    }
}
