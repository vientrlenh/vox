package com.sep.vox.infrastructure.repository;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;


import com.sep.vox.application.port.output.CacheManagerPort;

import tools.jackson.databind.json.JsonMapper;

@Repository
@RequiredArgsConstructor
@Slf4j
public class RedisCacheManagerRepository implements CacheManagerPort {

    /**
     * Built ONCE: {@code DefaultRedisScript} precomputes the script's SHA1 at construction, so creating a new one
     * on every call would hash the string again on every lock release -- unnecessary.
     */
    private static final DefaultRedisScript<Long> COMPARE_AND_DELETE = new DefaultRedisScript<>(
        "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
        Long.class
    );

    private final StringRedisTemplate redis;
    private final JsonMapper jsonMapper;

    @Override
    public void save(String key, String value) {
        redis.opsForValue().set(key, value);
    }

    @Override
    public void save(String key, String value, Duration ttl) {
        redis.opsForValue().set(key, value, ttl);
    }

    @Override
    public String get(String key) {
        return redis.opsForValue().get(key);
    }

    @Override
    public void delete(String key) {
        redis.delete(key);
    }

    @Override
    public void save(String key, Object value, Duration ttl) {
        redis.opsForValue().set(key, jsonMapper.writeValueAsString(value), ttl);
    }

    @Override
    public <T> T get(String key, Class<T> type) {
        String json = redis.opsForValue().get(key);
        return json == null ? null : jsonMapper.readValue(json, type);
    }

    @Override
    public Long getRemainingTtl(String key) {
        return redis.getExpire(key, TimeUnit.SECONDS);
    }

    @Override
    public String saveIfAbsentAndGet(String key, String value, Duration ttl) {
        Boolean isSaved = redis.opsForValue().setIfAbsent(key, value, ttl);
        return Boolean.TRUE.equals(isSaved) ? value : redis.opsForValue().get(key);
    }

    @Override
    public boolean deleteIfValueMatches(String key, String expectedValue) {
        if (key == null || expectedValue == null) {
            return false;
        }
        try {
            Long deleted = redis.execute(COMPARE_AND_DELETE, List.of(key), expectedValue);
            return deleted != null && deleted > 0;
        } catch (RuntimeException exception) {
            log.warn(
                "Could not release lock {} -- leaving it for the TTL to clean up.", key, exception
            );
            return false;
        }
    }
}
