package com.github.filipmalczak.percival;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.function.BiFunction;
import java.util.stream.Stream;

@Slf4j
@Component
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Tasks implements TaskExecutor {
    @Autowired
    DefinitionRepository definitionRepository;

    @Autowired
    RunRepository runRepository;

    @Autowired
    Session session;

//    ForkJoinPool pool = new ForkJoinPool();
    ExecutorService pool = Executors.newCachedThreadPool();

    TaskTreeBuilder root = new TaskTreeBuilder();

    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    private static class RetrievedTaskRun<T> implements TaskRun<T> {
        PersistentTaskRun<T> backend;

        @Override
        public ObjectId getRunId() {
            return backend.getRunId();
        }

        @Override
        public ObjectId getTaskId() {
            return backend.getTaskId();
        }

        @Override
        public RunStatus getStatus() {
            return RunStatus.FINISHED_SUCCESS;
        }

        @Override
        public Future<T> asFuture() {
            return new Future<T>() {
                @Override
                public boolean cancel(boolean mayInterruptIfRunning) {
                    return false;
                }

                @Override
                public boolean isCancelled() {
                    return false;
                }

                @Override
                public boolean isDone() {
                    return true;
                }

                @Override
                public T get() throws InterruptedException, ExecutionException {
                    return backend.getResult();
                }

                @Override
                public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
                    return get();
                }
            };
        }
    }

    private <T> void create(RuntimeRun<?, T> run){
        log.info("Persisting run "+run.runId);
        runRepository.save(
            new PersistentTaskRun<T>(
                run.runId,
                run.taskId,
                session.getId(),
                null,
                null,
                null,
                null,
                null
            )
        );
        log.info("Created run "+run.runId);
    }

    @SneakyThrows
    private void markStarted(RuntimeRun<?, ?> run){
        log.info("Starting run "+run.runId);
        run.status = RunStatus.STARTED;
        PersistentTaskRun<?> persistent = runRepository.findById(run.runId).get();
        persistent.setStartedOn(new Date());
        runRepository.save(persistent);
        log.info("Started run "+run.runId);
    }

    private <T> void markFinished(RuntimeRun<?, T> run, T result){
        log.info("Marking run "+run.runId+" as finished with success");
        run.status = RunStatus.FINISHED_SUCCESS;
        PersistentTaskRun<T> persistent = (PersistentTaskRun<T>) runRepository.findById(run.runId).get();
        persistent.setFinishedOn(new Date());
        persistent.setResult(result);
        persistent.setExitedWith(RunStatus.FINISHED_SUCCESS);
        persistent = runRepository.save(persistent);
        log.info("Run "+run.runId+" marked as finished with success");
        TaskDefinition<?> definition = definitionRepository.findById(run.taskId).get();
        definition.setSuccesfulRun(persistent);
        definitionRepository.save(definition);
        log.info("Task "+run.taskId+" updated with succesful run");
    }

    private void markException(RuntimeRun<?, ?> run, RuntimeException e){
        log.info("Marking run "+run.runId+" as finished with exception");
        run.status = RunStatus.FINISHED_EXCEPTION;
        PersistentTaskRun<?> persistent = runRepository.findById(run.runId).get();
        persistent.setFinishedOn(new Date());
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        persistent.setExceptionString(sw.toString());
        persistent.setExitedWith(RunStatus.FINISHED_EXCEPTION);
        runRepository.save(persistent);
        log.info("Run "+run.runId+" marked as finished with exception");
        log.error("EXCEPTION");
        e.printStackTrace();
    }

    private void markToBeContinued(RuntimeRun<?, ?> run){
        log.info("Marking run "+run.runId+" as to be continued");
        run.status = RunStatus.TO_BE_CONTINUED;
        PersistentTaskRun<?> persistent = runRepository.findById(run.runId).get();
        persistent.setFinishedOn(new Date()); //fixme should it stay null?
        persistent.setExitedWith(RunStatus.TO_BE_CONTINUED);
        runRepository.save(persistent);
        log.info("Run "+run.runId+" marked as to be continued");
    }

    @AllArgsConstructor
    @Data
    @FieldDefaults(level = AccessLevel.PRIVATE)
    private class RuntimeRun<P, T> implements TaskRun<T> {
        ObjectId runId;
        ObjectId taskId;
        RunStatus status;
        P parameters;
        BiFunction<P, TaskExecutor, T> body;
        Future<T> backend;

        public RuntimeRun(ObjectId runId, ObjectId taskId,  P parameters, BiFunction<P, TaskExecutor, T> body){
            this.runId = runId;
            this.taskId = taskId;
            this.status = RunStatus.PENDING;
            this.parameters = parameters;
            this.body = body;
        }

        void submit(){
//            this.backend = pool.submit(new RecursiveTask<T>() {
            this.backend = pool.submit(() -> {
                    markStarted(RuntimeRun.this);
                    TaskTreeBuilder executor = new TaskTreeBuilder();
                    executor.setParentId(taskId);
                    try {
                        T result = body.apply(parameters, executor);
                        executor.joinAll();
                        markFinished(RuntimeRun.this, result);
                        return result;
                    } catch (ToBeContinued e){
                        markToBeContinued(RuntimeRun.this);
                        throw e;
                    }
                    catch (RuntimeException e) {
                        markException(RuntimeRun.this, e);
                        throw e;
                    }
            });
        }

        @Override
        public Future<T> asFuture() {
            return backend;
        }
    }

    @Data
    @FieldDefaults(level = AccessLevel.PRIVATE)
    private class TaskTreeBuilder implements TaskExecutor {
        ObjectId parentId = null;
        List<TaskRun<?>> allRuns = new LinkedList<>();

        private <P> TaskDefinition<P> getDefinitionForSubTask(TaskKey<P> key){
            Optional<TaskDefinition<P>> existing = definitionRepository.findOneByKeyNameAndKeyParametersAndParentId(key.name, key.parameters, parentId);
            if (existing.isPresent())
                return existing.get();
            TaskDefinition<P> out = new TaskDefinition<>(null, key, parentId, session.getId(), new Date(), null);
            out = definitionRepository.save(out);
            return out;
        }


        private <T, P> TaskRun<T> createRun(TaskDefinition<P> definition, BiFunction<P, TaskExecutor, T> body) {
            if (definition.getSuccesfulRun() != null){
                log.info("Task "+definition.getKey()+" (#"+definition.getId()+") already executed in run #"+definition.getSuccesfulRun().getRunId());
                return new RetrievedTaskRun<T>((PersistentTaskRun<T>) definition.getSuccesfulRun());
            } else {
                log.info("Submitting new run for "+definition.getKey()+" (#"+definition.getId()+")");
                RuntimeRun<P, T> run = new RuntimeRun<>(
                    ObjectId.get(),
                    definition.getId(),
                    definition.getKey().getParameters(),
                    body
                );
                create(run);
                run.submit();
                return run;
            }
        }

        @Override
        public <T, P> TaskRun<T> task(TaskKey<P> key, BiFunction<P, TaskExecutor, T> body) {
            TaskDefinition<P> definition = getDefinitionForSubTask(key);
            TaskRun<T> run = createRun(definition, body);
            log.info("Obtained run "+run);
            allRuns.add(run);
            return run;
        }

        @Override
        public Stream<TaskRun<?>> getAllRuns() {
            return allRuns.stream();
        }
    }

    @Override
    public <T, P> TaskRun<T> task(TaskKey<P> key, BiFunction<P, TaskExecutor, T> body) {
        return root.task(key, body);
    }

    @Override
    public Stream<TaskRun<?>> getAllRuns() {
        return root.getAllRuns();
    }

    @PreDestroy
    public void cleanup(){
        joinAll();
        shutdown();
    }

    void shutdown(){
        pool.shutdown();
    }
}
