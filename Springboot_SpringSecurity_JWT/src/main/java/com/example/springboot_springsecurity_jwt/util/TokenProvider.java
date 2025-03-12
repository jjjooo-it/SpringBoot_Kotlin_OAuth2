package com.example.springboot_springsecurity_jwt.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Collections;
import java.util.Date;
import java.util.Set;

/**
 * TokenProvider 클래스
 * JWT 토큰 생성 및 유효성 검증 처리
 * AccessToken 및 RefreshToken 생성
 * Redis를 사용한 RefreshToken 저장 및 조회
 */
@RequiredArgsConstructor
@Component
public class TokenProvider {

    // JWT 발행자 정보
    @Value(value = "${jwt.issuer}")
    private String issuer;

    // AccessToken 서명을 위한 비밀키 (Base64 인코딩된 값으로 저장)
    @Value(value = "${jwt.secret.access}")
    private String accessSecretKey;

    // RefreshToken 서명을 위한 비밀키 (Base64 인코딩된 값으로 저장)
    @Value(value = "${jwt.secret.refresh}")
    private String refreshSecretKey;

    // RedisTemplate을 통한 Redis 접근 객체
    private final RedisTemplate<String, String> redisTemplate;

    // AccessToken 만료 시간 (10분)
    private static final long ACCESS_TOKEN_EXPIRE_TIME = 1000 * 60 * 10; // 10분
    // RefreshToken 만료 시간 (30일)
    private static final long REFRESH_TOKEN_EXPIRE_TIME = 1000L * 60 * 60 * 24 * 30; // 30일


    // AccessToken 생성 메서드
    public String makeAccessToken(Long memberId) {
        Date now = new Date();
        return Jwts.builder()
                .header()
                .add("typ", "JWT")
                .add("alg", "HS512")
                .and()
                .issuer(issuer)
                .subject(String.valueOf(memberId)) // subject에 memberId 설정
                .issuedAt(now)
                .expiration(new Date(now.getTime() + ACCESS_TOKEN_EXPIRE_TIME)) // 만료 시간 설정
                .claim("id", memberId)
                .signWith(getSigningKey(accessSecretKey))
                .compact();
    }

    // RefreshToken 생성 메서드
    public String makeRefreshToken(Long memberId) {
        Date now = new Date();
        String refreshToken = Jwts.builder()
                .header()
                .add("typ", "JWT")
                .add("alg", "HS512")
                .and()
                .issuer(issuer)
                .subject(String.valueOf(memberId)) // subject에 memberId 설정
                .issuedAt(now)
                .expiration(new Date(now.getTime() + REFRESH_TOKEN_EXPIRE_TIME)) // 만료 시간 설정
                .claim("id", memberId)
                .signWith(getSigningKey(refreshSecretKey))
                .compact();

        // Redis에 저장
        redisTemplate.opsForValue().set(
                String.valueOf(memberId),
                refreshToken,
                REFRESH_TOKEN_EXPIRE_TIME,
                java.util.concurrent.TimeUnit.MILLISECONDS
        );

        return refreshToken;
    }

    // AccessToken 유효성 검증 메서드
    // 유효하면 true, 그렇지 않으면 false 반환
    public boolean validateAccessToken(String token) {
        try {
            Jwts.parser()
                    .setSigningKey(getSigningKey(accessSecretKey)) // AccessToken 서명 키로 검증
                    .build()
                    .parse(token); // 토큰 파싱 및 검증
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // RefreshToken 유효성 검증 메서드
    // 유효하면 true, 그렇지 않으면 false 반환
    public boolean validateRefreshToken(String token) {
        try {
            Jwts.parser()
                    .setSigningKey(getSigningKey(refreshSecretKey)) // RefreshToken 서명 키로 검증
                    .build()
                    .parse(token); // 토큰 파싱 및 검증
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // 토큰 기반으로 인증 정보 생성
    public Authentication getAuthentication(String token) {
        Claims claims = getClaims(token, true); // AccessToken으로 인증 정보 생성

        Set<SimpleGrantedAuthority> authorities = Collections.singleton(new SimpleGrantedAuthority("ROLE_USER"));

        return new UsernamePasswordAuthenticationToken(
                new org.springframework.security.core.userdetails.User(claims.getSubject(), "", authorities),
                token,
                authorities
        );
    }

    // 토큰에서 memberId 추출
    public Long getMemberId(String token, boolean isAccessToken) {
        Claims claims = getClaims(token, isAccessToken);
        return claims.get("id", Long.class);
    }

    // 토큰에서 Claims 객체 추출
    public Claims getClaims(String token, boolean isAccessToken) {
        String secretKey = isAccessToken ? accessSecretKey : refreshSecretKey; // AccessToken과 RefreshToken에 맞는 서명 키 선택
        return Jwts.parser()
                .setSigningKey(getSigningKey(secretKey)) // 선택한 서명 키로 파싱
                .build()
                .parseSignedClaims(token)
                .getBody();
    }

    // Redis에서 RefreshToken 조회
    public String getRefreshTokenFromRedis(String loginId) {
        return redisTemplate.opsForValue().get(loginId);
    }

    // 서명 키를 Base64 디코딩하여 반환
    private Key getSigningKey(String secretKey) {
        return Keys.hmacShaKeyFor(java.util.Base64.getDecoder().decode(secretKey));
    }
}
