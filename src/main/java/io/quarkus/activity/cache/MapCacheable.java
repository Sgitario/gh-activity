package io.quarkus.activity.cache;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class MapCacheable<ID, T> {

    private final Function<ID, T> supplier;
    private final Map<ID, Cache<T>> map = new HashMap<>();

    public MapCacheable(Function<ID, T> supplier) {
        this.supplier = supplier;
    }

    public synchronized T get(ID repoId) {
        T value = null;
        Cache<T> item = map.get(repoId);
        if (item == null) {
            value = supplier.apply(repoId);
            map.put(repoId, new Cache<>(value));
        } else {
            // TODO: check if cache is expired!
            value = item.value;
        }

        return value;
    }

    public Set<ID> keys() {
        return Collections.unmodifiableSet(map.keySet());
    }

    private class Cache<T> {
        private final LocalDateTime createdAt;
        private final T value;

        public Cache(T value) {
            this.createdAt = LocalDateTime.now();
            this.value = value;
        }
    }
}
