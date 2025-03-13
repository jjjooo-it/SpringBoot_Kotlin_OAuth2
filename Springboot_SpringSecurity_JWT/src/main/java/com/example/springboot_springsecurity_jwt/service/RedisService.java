package com.example.springboot_springsecurity_jwt.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Redis 서비스 클래스
 * Redis에 데이터를 저장, 조회 및 삭제하는 기능 제공
 */
@Service
@RequiredArgsConstructor
public class RedisService {
    private final RedisTemplate<String, Object> redisTemplate;

    // Redis에 데이터를 저장 (만료 시간 설정)
    public void saveValue(String key, String value, long duration, TimeUnit timeUnit) {
        redisTemplate.opsForValue().set(key, value, duration, timeUnit);
    }

    // Redis에서 데이터를 조회
    public String getValue(String key) {
        return (String) redisTemplate.opsForValue().get(key);
    }

    // Redis에서 데이터를 삭제
    public void deleteValue(String key) {
        redisTemplate.delete(key);
    }
}
