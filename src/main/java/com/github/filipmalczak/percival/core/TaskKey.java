package com.github.filipmalczak.percival.core;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;

@Data
@Setter(AccessLevel.PRIVATE)
public class TaskKey<Parameters> {
    String name;
    Parameters parameters = null;
    int parametersHash = 0;

    public static <T> int getHash(T t){
        return t == null ? 0 : t.hashCode();
    }

    public TaskKey(String name, Parameters parameters) {
        this.name = name != null ? name.trim() : null;

        // I don't know why, but Tasks implementation is going nuts when parameter is String.
        // Considering that if you need just a string, you can use name with null parameters, let's block the possibility
        // to do that.
        if (parameters instanceof String)
            throw new RuntimeException(); //todo dedicated exception

        this.parameters = parameters;
        this.parametersHash = getHash(parameters);
    }

    public static TaskKey<Void> of(String name){
        return new TaskKey<>(name, null);
    }

    public static <P> TaskKey<P> of(P parameters){
        return new TaskKey<>(null, parameters);
    }
}
