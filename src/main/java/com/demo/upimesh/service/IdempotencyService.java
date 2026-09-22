package com.demo.upimesh.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class IdempotencyService {

    private static final String KEY_PREFIX = "upi:idempotency:";

    private final StringRedisTemplate redisTemplate;

    @Value("${upi.mesh.idempotency-ttl-seconds:86400}")
    private long ttlSeconds;

    public IdempotencyService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Atomically claims a packet hash using Redis SETNX semantics.
     *
     * Returns true only for the first caller.
     * Returns false when the packet has already been processed
     * within the configured TTL window.
     */
    public boolean claim(String packetHash) {
        String key = KEY_PREFIX + packetHash;

        Boolean claimed = redisTemplate.opsForValue()
                .setIfAbsent(key, "1", Duration.ofSeconds(ttlSeconds));

        return Boolean.TRUE.equals(claimed);
    }

    /**
     * Returns the number of currently tracked idempotency keys.
     */
    public int size() {
        var keys = redisTemplate.keys(KEY_PREFIX + "*");
        return keys == null ? 0 : keys.size();
    }

    /**
     * Clears all idempotency keys.
     * Used by the demo/reset endpoint.
     */
    public void clear() {
        var keys = redisTemplate.keys(KEY_PREFIX + "*");

        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }
}