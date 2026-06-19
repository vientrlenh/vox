package com.sep.vox.infrastructure.repository;

import java.time.Duration;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;


import com.sep.vox.application.port.output.CacheManagerPort;

import tools.jackson.databind.json.JsonMapper;

@Repository
public class RedisCacheManagerRepository implements CacheManagerPort {

    private final StringRedisTemplate redis;
    private final JsonMapper jsonMapper;

    public RedisCacheManagerRepository(StringRedisTemplate redis, JsonMapper jsonMapper) {
        this.redis = redis;
        this.jsonMapper = jsonMapper;
    }

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
        var json = redis.opsForValue().get(key);
        return json == null ? null : jsonMapper.readValue(json, type);
    }
}
