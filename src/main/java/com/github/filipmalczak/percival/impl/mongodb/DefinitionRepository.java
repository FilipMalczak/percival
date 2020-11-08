package com.github.filipmalczak.percival.impl.mongodb;

import com.github.filipmalczak.percival.core.PercivalException;
import org.bson.types.ObjectId;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static com.github.filipmalczak.percival.core.TaskKey.getHash;
import static java.util.stream.Collectors.toList;

@Repository
public interface DefinitionRepository extends CrudRepository<TaskDefinition<?>, ObjectId> {
//    <Parameters> Optional<TaskDefinition<Parameters>> findOneByKeyAndParentId(TaskKey<Parameters> key, ObjectId parentId);
    Stream<TaskDefinition<?>> findAllByKeyNameAndKeyParametersHashAndParentId(String name, int parametersHash, ObjectId parentId);

    static boolean nullSafeEquals(Object o, Object b){
        if (o != null)
            return o.equals(b);
        return b == null;
    }

    default <Parameters> Optional<TaskDefinition<Parameters>> findOneByKeyNameAndKeyParametersAndParentId(String name, Parameters parameters, ObjectId parentId){
        List<TaskDefinition<?>> hashCollision = findAllByKeyNameAndKeyParametersHashAndParentId(name, getHash(parameters), parentId)
            .filter(d -> nullSafeEquals(d.getKey().getParameters(), parameters))
            .collect(toList());
        if (hashCollision.size() > 1)
            throw new PercivalException();
        return hashCollision.stream().map(x -> (TaskDefinition<Parameters>) x).findFirst();
    }
}
