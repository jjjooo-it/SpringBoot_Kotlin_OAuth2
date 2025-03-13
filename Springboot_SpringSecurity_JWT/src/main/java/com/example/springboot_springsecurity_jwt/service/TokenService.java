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

    private final RedisService redisService;

    // 생성자를 통해 RedisTemplate 주입
    public TokenService(RedisService redisService) {
        this.redisService = redisService;
    }

    // 리프레시 토큰을 Redis에 저장
    // 저장 기간은 7일로 설정
    public void saveRefreshToken(Long memberId, String refreshToken) {
        redisService.saveValue("RT:" + memberId, refreshToken);
    }

    // Redis에서 리프레시 토큰 조회
    public String getRefreshToken(Long memberId) {
        return redisService.getValue("RT:" + memberId);
    }

    // Redis에서 리프레시 토큰 삭제
    public void deleteRefreshToken(Long memberId) {
        redisService.deleteValue("RT:" + memberId);
    }
}
