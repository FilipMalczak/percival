package com.github.filipmalczak.percival.impl.mongodb;

import com.github.filipmalczak.percival.core.RunStatus;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@AllArgsConstructor
@Data
@Document
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PersistentTaskRun<T> {
    @Id
    ObjectId runId;
    ObjectId taskId;
    ObjectId sessionId;

    Date startedOn;
    Date finishedOn;

    RunStatus exitedWith;

    T result = null;
    String exceptionString = null;
}
