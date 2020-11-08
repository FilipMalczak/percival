package com.github.filipmalczak.percival;

import org.bson.types.ObjectId;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RunRepository extends CrudRepository<PersistentTaskRun<?>, ObjectId> {
}
