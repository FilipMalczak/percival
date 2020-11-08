package com.github.filipmalczak.percival.impl.mongodb;

import com.github.filipmalczak.percival.core.Session;
import org.springframework.data.repository.CrudRepository;

public interface SessionDataRepository extends CrudRepository<Session.PersistentData, String> {
}
