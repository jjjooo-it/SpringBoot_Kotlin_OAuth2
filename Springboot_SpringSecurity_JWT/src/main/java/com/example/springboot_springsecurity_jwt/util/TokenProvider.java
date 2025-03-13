package com.example.springboot_springsecurity_jwt.util;

import com.example.springboot_springsecurity_jwt.service.RedisService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import java.util.Collections;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * TokenProvider 클래스
 * JWT 토큰 생성 및 유효성 검증 처리
 * AccessToken 및 RefreshToken 생성
 * Redis를 사용한 RefreshToken 저장 및 조회
 */
@RequiredArgsConstructor
@Component
public class TokenProvider {

    @Value("${jwt.issuer}")
    private String issuer;

    @Value("${jwt.secret.access}")
    private String accessSecretKey;

    @Value("${jwt.secret.refresh}")
    private String refreshSecretKey;

    private final RedisService redisService;

    private static final long ACCESS_TOKEN_EXPIRE_TIME = 1000 * 60 * 10; // 10분
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
                .subject(String.valueOf(memberId))
                .issuedAt(now)
                .expiration(new Date(now.getTime() + ACCESS_TOKEN_EXPIRE_TIME))
                .claim("id", memberId)
                .signWith(accessSecretKey)
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
                .subject(String.valueOf(memberId))
                .issuedAt(now)
                .expiration(new Date(now.getTime() + REFRESH_TOKEN_EXPIRE_TIME))
                .claim("id", memberId)
                .signWith(refreshSecretKey)
                .compact();

        // Redis에 저장 (만료 시간 설정)
        redisService.saveValue("RT:" + memberId, refreshToken, REFRESH_TOKEN_EXPIRE_TIME, TimeUnit.MILLISECONDS);

        return refreshToken;
    }

    // AccessToken 유효성 검증 메서드
    public boolean validateAccessToken(String token) {
        try {
            Jwts.parser()
                    .setSigningKey(accessSecretKey)
                    .build()
                    .parse(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // RefreshToken 유효성 검증 메서드
    public boolean validateRefreshToken(String token) {
        try {
            Jwts.parser()
                    .setSigningKey(refreshSecretKey)
                    .build()
                    .parse(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // 토큰 기반으로 인증 정보 생성
    public Authentication getAuthentication(String token) {
        Claims claims = getClaims(token, true);

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
        String secretKey = isAccessToken ? accessSecretKey : refreshSecretKey;
        return Jwts.parser()
                .setSigningKey(secretKey)
                .build()
                .parseSignedClaims(token)
                .getBody();
    }

    // Redis에서 RefreshToken 조회
    public String getRefreshTokenFromRedis(Long memberId) {
        return redisService.getValue("RT:" + memberId); // RedisService 사용
    }

}
