package com.github.filipmalczak.percival.impl.mongodb;

import com.github.filipmalczak.percival.api.Task;
import com.github.filipmalczak.percival.core.*;
import lombok.*;
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
public class Tasks implements Task {
    @Autowired
    DefinitionRepository definitionRepository;

    @Autowired
    RunRepository runRepository;

    @Autowired
    Session session;

    @Getter
    @Setter
    TaskExecutor currentExecutor = TaskExecutors.currentThread();

    TaskTreeBuilder root = new TaskTreeBuilder(getCurrentExecutor());

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
        BiFunction<P, Task, T> body;
        TaskExecutor executor;
        Future<T> backend;

        public RuntimeRun(ObjectId runId, ObjectId taskId,  P parameters, BiFunction<P, Task, T> body, TaskExecutor executor){
            this.runId = runId;
            this.taskId = taskId;
            this.status = RunStatus.PENDING;
            this.parameters = parameters;
            this.body = body;
            this.executor = executor;
        }

        void submit(){
//            this.backend = pool.submit(new RecursiveTask<T>() {
            this.backend = executor.execute(() -> {
                    markStarted(RuntimeRun.this);
                    TaskTreeBuilder task = new TaskTreeBuilder(this.executor);
                    task.setParentId(taskId);
                    try {
                        T result = body.apply(parameters, task);
                        task.joinAll();
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
    private class TaskTreeBuilder implements Task {
        ObjectId parentId = null;
        List<TaskRun<?>> allRuns = new LinkedList<>();
        TaskExecutor currentExecutor;

        public TaskTreeBuilder(TaskExecutor currentExecutor) {
            this.currentExecutor = currentExecutor;
        }

        private <P> TaskDefinition<P> getDefinitionForSubTask(TaskKey<P> key){
            Optional<TaskDefinition<P>> existing = definitionRepository.findOneByKeyNameAndKeyParametersAndParentId(key.getName(), key.getParameters(), parentId);
            if (existing.isPresent())
                return existing.get();
            TaskDefinition<P> out = new TaskDefinition<>(null, key, parentId, session.getId(), new Date(), null);
            out = definitionRepository.save(out);
            return out;
        }


        private <T, P> TaskRun<T> createRun(TaskDefinition<P> definition, BiFunction<P, Task, T> body) {
            if (definition.getSuccesfulRun() != null){
                log.info("Task "+definition.getKey()+" (#"+definition.getId()+") already executed in run #"+definition.getSuccesfulRun().getRunId());
                return new RetrievedTaskRun<T>((PersistentTaskRun<T>) definition.getSuccesfulRun());
            } else {
                log.info("Submitting new run for "+definition.getKey()+" (#"+definition.getId()+")");
                RuntimeRun<P, T> run = new RuntimeRun<>(
                    ObjectId.get(),
                    definition.getId(),
                    definition.getKey().getParameters(),
                    body,
                    currentExecutor
                );
                create(run);
                run.submit();
                return run;
            }
        }

        @Override
        public <T, P> TaskRun<T> createTask(TaskKey<P> key, BiFunction<P, Task, T> body) {
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
    public <T, P> TaskRun<T> createTask(TaskKey<P> key, BiFunction<P, Task, T> body) {
        return root.createTask(key, body);
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
        currentExecutor.shutdown();
        //todo some executors might leak
        getAllRuns().forEach(r -> {
            if (r instanceof RuntimeRun) {
                RuntimeRun runtimeRun = (RuntimeRun) r;
                runtimeRun.getExecutor().shutdown();
            }
        });
    }
}
