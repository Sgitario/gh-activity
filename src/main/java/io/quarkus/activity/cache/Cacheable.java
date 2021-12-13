package io.quarkus.activity.cache;

import java.time.LocalDateTime;
import java.util.function.Supplier;

public class Cacheable<T> {
    private final Supplier<T> loadValue;
    private LocalDateTime createdAt;
    private T value;

    public Cacheable(Supplier<T> loadValue) {
        this.loadValue = loadValue;
    }

    public T get() {
        if (value == null) { // TODO: check whether cache is expired!
            createdAt = LocalDateTime.now();
            value = loadValue.get();
        }

        return value;
    }
}
