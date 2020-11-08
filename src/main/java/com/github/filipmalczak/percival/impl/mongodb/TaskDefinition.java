package com.github.filipmalczak.percival.impl.mongodb;

import com.github.filipmalczak.percival.core.TaskKey;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Data
@Document
@FieldDefaults(level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
@AllArgsConstructor
@NoArgsConstructor
public class TaskDefinition<Parameters> {
    @Id
    ObjectId id;

    @Indexed
    @NonNull TaskKey<Parameters> key;
    //todo or dbref?
    @Indexed
    ObjectId parentId;


    @NonNull ObjectId sessionIdWhenFirstDefined;
    @NonNull Date dateWhenFirstDefined;

    @DBRef
    PersistentTaskRun<?> succesfulRun = null;

}
