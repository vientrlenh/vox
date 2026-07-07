package com.sep.vox.application.port.output;

import java.time.Duration;

public interface CacheManagerPort {
    void save(String key, String value);
    void save(String key, String value, Duration ttl);
    String get(String key);
    void delete(String key);

    void save(String key, Object value, Duration ttl);
    <T> T get(String key, Class<T> type);
    Long getRemainingTtl(String key);
    String saveIfAbsentAndGet(String key, String value, Duration ttl);
}
