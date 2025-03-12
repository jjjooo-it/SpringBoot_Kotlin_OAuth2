package com.example.springboot_springsecurity_jwt.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * TokenService 클래스
 * Redis에 리프레시 토큰을 저장, 조회 및 삭제하는 기능 제공
 */
@Service
public class TokenService {

    // Redis 서버와 통신하기 위한 RedisTemplate 객체
    private final RedisTemplate<String, String> redisTemplate;

    // 생성자를 통해 RedisTemplate 주입
    public TokenService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // 리프레시 토큰을 Redis에 저장
    // 저장 기간은 7일로 설정
    public void saveRefreshToken(Long memberId, String refreshToken) {
        redisTemplate.opsForValue().set("RT:" + memberId, refreshToken, Duration.ofDays(7));
    }

    // Redis에서 리프레시 토큰 조회
    public String getRefreshToken(Long memberId) {
        return redisTemplate.opsForValue().get("RT:" + memberId);
    }

    // Redis에서 리프레시 토큰 삭제
    public void deleteRefreshToken(Long memberId) {
        boolean status = redisTemplate.delete("RT:" + memberId);
    }
}
