package com.example.springboot_springsecurity_jwt.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * TokenService 클래스
 * Redis에 리프레시 토큰을 저장, 조회 및 삭제하는 기능 제공
 */
@Service
@RequiredArgsConstructor
public class TokenService {

    private final RedisService redisService;

    // 리프레시 토큰을 Redis에 저장
    public void saveRefreshToken(Long memberId, String refreshToken) {
        redisService.saveValue("RT:" + memberId, refreshToken, 7, TimeUnit.DAYS); // 7일 저장
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
