package com.github.filipmalczak.percival;

import org.springframework.data.repository.CrudRepository;

public interface SessionDataRepository extends CrudRepository<Session.PersistentData, String> {
}
