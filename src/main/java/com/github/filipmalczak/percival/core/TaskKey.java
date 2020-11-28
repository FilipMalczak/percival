package com.github.filipmalczak.percival.core;

import lombok.*;

import java.util.Collection;
import java.util.Map;

import static org.springframework.beans.BeanUtils.isSimpleValueType;

/**
 * todo consider support for maps
 * @param <Parameters> non-simple, non-void, non-collection and non-array parameters type; cannot be internal class (must pubnlicly reside in package and not another class); must be embeddable in MongoDB document
 */
@Data
@Setter(AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TaskKey<Parameters> {
    String name;
    Parameters parameters;

    public static <T> boolean canActAsParameters(@NonNull Class<T> clazz){
        if (isSimpleValueType(clazz))
            return false;
        if (clazz.isArray())
            return false;
        if (Collection.class.isAssignableFrom(clazz))
            return false;
        if (Map.class.isAssignableFrom(clazz))
            return false;
        if (clazz.equals(void.class))
            return false;
        if (clazz.equals(Void.class))
            return false;
        if (clazz.getDeclaringClass() != null)
            return false;
        return true;
    }

    public static <P> TaskKey<P> of(String name, P parameters){
        if (parameters != null && ! canActAsParameters(parameters.getClass()))
            throw new RuntimeException(); //todo dedicated exception
        return new TaskKey<>(name, parameters);
    }

    public static TaskKey<Void> of(String name){
        return of(name, null);
    }

    public static <P> TaskKey<P> of(P parameters){
        return of(null, parameters);
    }
}
