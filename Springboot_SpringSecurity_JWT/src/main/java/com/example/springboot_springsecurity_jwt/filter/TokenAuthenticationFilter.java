package com.example.springboot_springsecurity_jwt.filter;

import com.example.springboot_springsecurity_jwt.service.TokenService;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * TokenAuthenticationFilter 클래스
 * 매 요청마다 실행되는 필터로, JWT 토큰의 인증 및 갱신 처리
 */
@RequiredArgsConstructor
public class TokenAuthenticationFilter extends OncePerRequestFilter {

    private final TokenService tokenService;

    // HTTP 헤더에서 토큰을 추출할 때 사용할 이름
    private static final String TOKEN_HEADER = "Authorization";
    // Bearer 타입의 토큰을 의미하는 접두사
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String token = extractToken(request); // 요청에서 JWT 토큰을 추출

        try {
            if (tokenService.validateAccessToken(token)) { // 토큰이 유효한지 검증
                Authentication authentication = tokenService.getAuthentication(token); // 토큰을 기반으로 인증 정보 생성
                SecurityContextHolder.getContext().setAuthentication(authentication); // 인증 정보를 SecurityContext에 저장
            }
        } catch (ExpiredJwtException e) {
            handleExpiredToken(e, response); // 만료된 토큰 처리
        }

        filterChain.doFilter(request, response); // 필터 체인 실행
    }

    // HTTP 요청에서 JWT 토큰을 추출하는 메서드
    private String extractToken(HttpServletRequest request) {
        String authorizationHeader = request.getHeader(TOKEN_HEADER); // Authorization 헤더에서 토큰 추출
        if (authorizationHeader != null && authorizationHeader.startsWith(BEARER_PREFIX)) {  // Bearer 타입의 토큰인지 확인
            return authorizationHeader.substring(BEARER_PREFIX.length());  // Bearer 접두사 이후의 토큰 반환
        }
        return ""; // 토큰이 없으면 빈 문자열 반환
    }

    // 만료된 토큰을 처리하는 메서드
    private void handleExpiredToken(ExpiredJwtException e, HttpServletResponse response) {
        Long memberId = e.getClaims().get("id", Long.class); // 만료된 토큰에서 memberId 추출

        // Redis에서 해당 memberId의 refreshToken 조회
        String refreshToken = tokenService.getRefreshTokenFromRedis(memberId);
        if (tokenService.validateRefreshToken(refreshToken)) { // refreshToken이 유효한지 확인
            // 새 accessToken을 생성하고 인증 정보 갱신
            String newAccessToken = tokenService.makeAccessToken(memberId);
            Authentication authentication = tokenService.getAuthentication(newAccessToken);
            SecurityContextHolder.getContext().setAuthentication(authentication);  // 새 accessToken으로 인증 정보 설정

            response.setHeader("New-Access-Token", newAccessToken); // 응답 헤더에 새로운 accessToken 추가
        }
    }
}
