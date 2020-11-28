package com.github.filipmalczak.percival.impl.mongodb.parameters;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class IntString {
    int anInt;
    String string;
}
