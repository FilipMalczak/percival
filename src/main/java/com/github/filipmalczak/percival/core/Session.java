package com.github.filipmalczak.percival.core;

import com.github.filipmalczak.percival.impl.mongodb.PersistentTaskRun;
import com.github.filipmalczak.percival.impl.mongodb.SessionDataRepository;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Optional;

import static com.github.filipmalczak.percival.core.Session.PersistentData.CURRENT_DATA_ID;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

@Component
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Session {
    @Document
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PersistentData {
        public static String CURRENT_DATA_ID = "currentSessionData";

        @Id
        String id=CURRENT_DATA_ID;

        ObjectId currentSessionId;
    }

    @Autowired
    SessionDataRepository sessionDataRepository;

    @Getter private final ObjectId id = ObjectId.get();

    @Autowired
    MongoTemplate template;

    @PostConstruct
    public void setup(){
        Optional<PersistentData> data = sessionDataRepository.findById(CURRENT_DATA_ID);
        PersistentData toSave = data.orElse(new PersistentData());
        if (data.isPresent()){
            interruptPrevious(data.get().currentSessionId);
        }
        toSave.currentSessionId = getId();
        sessionDataRepository.save(toSave);
    }

    private void interruptPrevious(ObjectId toInterrupt) {
        template.updateMulti(
            query(where("sessionId").ne(getId()).and("exitedWith").is(null)),
//            query(where("sessionId").is(toInterrupt).and("exitedWith").is(null)),
            new Update().set("exitedWith", RunStatus.INTERRUPTED),
            PersistentTaskRun.class
        );
    }
}
