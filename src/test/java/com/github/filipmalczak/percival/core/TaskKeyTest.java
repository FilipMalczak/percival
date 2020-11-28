package com.github.filipmalczak.percival.core;

import com.github.filipmalczak.percival.impl.mongodb.parameters.IntString;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class TaskKeyTest {

    @Test
    //todo unroll
    void canActAsParameters() {
        //todo longs, floats, doubles, sets, other impls, maps
        assertTrue(TaskKey.canActAsParameters(IntString.class));
        assertFalse(TaskKey.canActAsParameters(Void.class));
        assertFalse(TaskKey.canActAsParameters(String.class));
        assertFalse(TaskKey.canActAsParameters(Class.class));
        assertFalse(TaskKey.canActAsParameters(Integer.class));
        assertFalse(TaskKey.canActAsParameters(Boolean.class));
        assertFalse(TaskKey.canActAsParameters(Collection.class));
        assertFalse(TaskKey.canActAsParameters(List.class));
        assertFalse(TaskKey.canActAsParameters(ArrayList.class));
        assertFalse(TaskKey.canActAsParameters(Map.class));
        assertFalse(TaskKey.canActAsParameters(HashMap.class));
        assertFalse(TaskKey.canActAsParameters(void.class));
        assertFalse(TaskKey.canActAsParameters(int.class));
        assertFalse(TaskKey.canActAsParameters(boolean.class));
        assertFalse(TaskKey.canActAsParameters(int[].class));
        assertFalse(TaskKey.canActAsParameters(String[].class));
        assertFalse(TaskKey.canActAsParameters(List[].class));
        assertFalse(TaskKey.canActAsParameters(ArrayList[].class));
        assertFalse(TaskKey.canActAsParameters(Collection[].class));
    }
}