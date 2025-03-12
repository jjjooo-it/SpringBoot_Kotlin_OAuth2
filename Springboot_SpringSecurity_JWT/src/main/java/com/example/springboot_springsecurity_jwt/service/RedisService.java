package com.example.springboot_springsecurity_jwt.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Redis 서비스 클래스
 * Redis에 데이터를 저장, 조회 및 삭제하는 기능 제공
 */
@Service
public class RedisService {
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    //Redis에 값을 저장하는 메서드
    public void saveValue(String key, String value) {
        redisTemplate.opsForValue().set(key, value);
    }

    //Redis에서 값을 조회하는 메서드
    public String getValue(String key) {
        return (String) redisTemplate.opsForValue().get(key);
    }

    // Redis에서 값을 삭제하는 메서드
    public void deleteValue(String key) {
        redisTemplate.delete(key);
    }
}
