package com.sep.vox.infrastructure.repository;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;


import com.sep.vox.application.port.output.CacheManagerPort;

import tools.jackson.databind.json.JsonMapper;

@Repository
public class RedisCacheManagerRepository implements CacheManagerPort {

    /**
     * Dựng MỘT lần: {@code DefaultRedisScript} tính sẵn SHA1 của script lúc khởi tạo, nên tạo mới
     * mỗi lần gọi là băm lại chuỗi mỗi lần nhả khoá -- không cần thiết.
     */
    private static final DefaultRedisScript<Long> COMPARE_AND_DELETE = new DefaultRedisScript<>(
        "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
        Long.class
    );

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

    @Override
    public Long getRemainingTtl(String key) {
        return redis.getExpire(key, TimeUnit.SECONDS);
    }

    @Override
    public String saveIfAbsentAndGet(String key, String value, Duration ttl) {
        var isSaved = redis.opsForValue().setIfAbsent(key, value, ttl);
        return Boolean.TRUE.equals(isSaved) ? value : redis.opsForValue().get(key);
    }

    /**
     * So sánh rồi xoá trong MỘT lệnh Redis, bằng script Lua -- Redis chạy script đơn luồng nên
     * không lệnh nào chen được vào giữa GET và DEL.
     *
     * <p>Script tự viết thay vì dùng {@code WATCH/MULTI} vì cách kia đòi giữ nguyên một connection
     * qua nhiều lệnh, mà pool của Lettuce thì không đảm bảo điều đó.
     */
    @Override
    public boolean deleteIfValueMatches(String key, String expectedValue) {
        if (key == null || expectedValue == null) {
            return false;
        }
        Long deleted = redis.execute(COMPARE_AND_DELETE, List.of(key), expectedValue);
        return deleted != null && deleted > 0;
    }
}
