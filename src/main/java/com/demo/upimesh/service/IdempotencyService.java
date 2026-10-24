package com.demo.upimesh.service;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class IdempotencyService {

    private final ConcurrentHashMap<String, Instant> seen =
            new ConcurrentHashMap<>();

    public boolean claim(String packetHash) {
        return seen.putIfAbsent(packetHash, Instant.now()) == null;
    }

    public void clear() {
        seen.clear();
    }
    public int size() {
        return seen.size();
    }
}