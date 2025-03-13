package com.example.springboot_springsecurity_jwt.filter;

import com.example.springboot_springsecurity_jwt.util.TokenProvider;
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

    // 토큰 관리를 위한 TokenProvider 객체
    private final TokenProvider tokenProvider;

    // HTTP 헤더에서 토큰이 담길 키 값
    private static final String TOKEN_HEADER = "Authorization";
    // Bearer 타입의 토큰 접두사
    private final static String BEARER_PREFIX = "Bearer ";
    // Redis에 저장된 RefreshToken의 접두사
    private static final String REFRESH_TOKEN_PREFIX = "RT:";

    /**
     * 요청이 들어올 때마다 실행되는 메서드
     * - JWT 유효성 검사 및 인증 처리
     * - AccessToken이 만료된 경우 RefreshToken으로 갱신 처리
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String token = "";
        String authorizationHeader;

        // 요청 헤더에서 Authorization 키의 값 조회
        authorizationHeader = request.getHeader(TOKEN_HEADER);

        // Authorization 헤더가 존재하고 Bearer 타입인지 확인
        if (authorizationHeader != null && authorizationHeader.startsWith(BEARER_PREFIX)) {
            // "Bearer " 접두사 제거 후 순수 토큰 값 추출
            token = authorizationHeader.substring(BEARER_PREFIX.length());
        }

        try {
            // 추출된 토큰의 유효성을 검증
            if (tokenProvider.validateAccessToken(token)) {
                // 유효한 경우 인증 정보 생성 및 SecurityContext 설정
                Authentication authentication = tokenProvider.getAuthentication(token);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (ExpiredJwtException e) {
            // AccessToken이 만료된 경우
            Long memberId = e.getClaims().get("memberId", Long.class);

            // Redis에서 RefreshToken 조회
            String refreshToken = tokenProvider.getRefreshTokenFromRedis(Long.valueOf(REFRESH_TOKEN_PREFIX + memberId));

            // RefreshToken 유효성 확인 후 새로운 AccessToken 생성 및 설정
            if (tokenProvider.validateRefreshToken(refreshToken)) {
                String newAccessToken = tokenProvider.makeAccessToken(memberId);
                Authentication authentication = tokenProvider.getAuthentication(newAccessToken);
                SecurityContextHolder.getContext().setAuthentication(authentication);

                // 새 AccessToken을 응답 헤더에 추가 (클라이언트에서 저장 필요)
                response.setHeader("New-Access-Token", newAccessToken);
            }
        }

        // 다음 필터로 요청 전달
        filterChain.doFilter(request, response);
    }
}
