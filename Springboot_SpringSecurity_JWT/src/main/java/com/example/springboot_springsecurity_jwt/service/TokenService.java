package com.example.springboot_springsecurity_jwt.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Collections;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.TimeUnit;


/**
 * TokenService 클래스
 * AccessToken 및 RefreshToken 생성
 * JWT 토큰 생성 및 유효성 검증 처리
 * Redis를 사용한 RefreshToken 저장 및 조회
 */
@Component
@RequiredArgsConstructor
public class TokenService {

    // application.yml 에서 jwt 설정 값 주입
    @Value("${jwt.issuer}")
    private String issuer;

    @Value("${jwt.secret.access}")
    private String accessSecretKey;

    @Value("${jwt.secret.refresh}")
    private String refreshSecretKey;

    private final RedisService redisService;

    // AccessToken 만료 시간 (10분)
    private static final long ACCESS_TOKEN_EXPIRE_TIME = 1000 * 60 * 10;

    // RefreshToken 만료 시간 (30일)
    private static final long REFRESH_TOKEN_EXPIRE_TIME = 1000L * 60 * 60 * 24 * 30;


    // 공통 JWT 빌더 메서드
    private String createToken(Long memberId, String secretKey, long expireTime) {
        Date now = new Date();
        Key key = Keys.hmacShaKeyFor(secretKey.getBytes());
        return Jwts.builder()
                .header()
                .add("typ", "JWT")  // 토큰 타입
                .add("alg", "HS512") // 시그니처 알고리즘
                .and()
                .issuer(issuer)
                .subject(String.valueOf(memberId)) // 회원 ID
                .issuedAt(now) // 발행일
                .expiration(new Date(now.getTime() + expireTime)) // 만료일
                .claim("id", memberId) // 회원 ID
                .signWith(key)
                .compact();
    }

    // AccessToken 생성 메서드
    public String makeAccessToken(Long memberId) {
        return createToken(memberId, accessSecretKey, ACCESS_TOKEN_EXPIRE_TIME);
    }

    // RefreshToken 생성 메서드
    public String makeRefreshToken(Long memberId) {
        String refreshToken = createToken(memberId, refreshSecretKey, REFRESH_TOKEN_EXPIRE_TIME);
        // redis 에 저장
        redisService.saveValue("RT:" + memberId, refreshToken, REFRESH_TOKEN_EXPIRE_TIME, TimeUnit.MILLISECONDS);
        return refreshToken;
    }

    // AccessToken 유효성 검증 메서드
    public boolean validateAccessToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(Keys.hmacShaKeyFor(accessSecretKey.getBytes()))
                    .build()
                    .parse(token); // 토큰 파싱
            return true; // 유효한 토큰
        } catch (Exception e) {
            return false; // 유효하지 않은 토큰
        }
    }

    // RefreshToken 유효성 검증 메서드
    public boolean validateRefreshToken(String refreshToken) {
        try {
            Jwts.parser()
                    .verifyWith(Keys.hmacShaKeyFor(refreshSecretKey.getBytes()))
                    .build()
                    .parse(refreshToken);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // 토큰에서 Claims 객체 추출
    public Claims getClaims(String token, boolean isAccessToken) {
        String secretKey = isAccessToken ? accessSecretKey : refreshSecretKey;
        return Jwts.parser()
                .setSigningKey(Keys.hmacShaKeyFor(secretKey.getBytes()))
                .build()
                .parseSignedClaims(token)
                .getBody();
    }

    // 토큰 기반으로 인증 정보 생성
    public Authentication getAuthentication(String token) {
        Claims claims = getClaims(token, true); // AccessToken이므로 true
        Set<SimpleGrantedAuthority> authorities = Collections.singleton(new SimpleGrantedAuthority("ROLE_USER"));

        return new UsernamePasswordAuthenticationToken(
                new org.springframework.security.core.userdetails.User(claims.getSubject(), "", authorities),
                token,
                authorities
        );
    }

    // Redis에서 RefreshToken 조회
    public String getRefreshTokenFromRedis(Long memberId) {
        return redisService.getValue("RT:" + memberId);
    }

}

