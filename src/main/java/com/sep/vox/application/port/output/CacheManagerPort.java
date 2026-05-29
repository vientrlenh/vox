package com.sep.vox.application.port.output;

import java.time.Duration;

public interface CacheManagerPort {
    void save(String key, String value);
    void save(String key, String value, Duration ttl);
    String get(String key);
    void delete(String key);
}
