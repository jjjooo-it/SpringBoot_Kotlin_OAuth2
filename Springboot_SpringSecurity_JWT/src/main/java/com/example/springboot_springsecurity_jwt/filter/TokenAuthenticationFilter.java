package com.example.springboot_springsecurity_jwt.filter;

import com.example.springboot_springsecurity_jwt.service.TokenService;
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

    private final TokenProvider tokenProvider;
    private final TokenService tokenService;

    private static final String TOKEN_HEADER = "Authorization";
    private final static String BEARER_PREFIX = "Bearer ";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String token = "";
        String authorizationHeader = request.getHeader(TOKEN_HEADER);

        if (authorizationHeader != null && authorizationHeader.startsWith(BEARER_PREFIX)) {
            token = authorizationHeader.substring(BEARER_PREFIX.length());
        }

        try {
            if (tokenProvider.validateAccessToken(token)) {
                Authentication authentication = tokenProvider.getAuthentication(token);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (ExpiredJwtException e) {
            Long memberId = e.getClaims().get("id", Long.class);

            String refreshToken = tokenService.getRefreshToken(memberId);
            if (tokenProvider.validateRefreshToken(refreshToken)) {
                String newAccessToken = tokenProvider.makeAccessToken(memberId);
                Authentication authentication = tokenProvider.getAuthentication(newAccessToken);
                SecurityContextHolder.getContext().setAuthentication(authentication);

                response.setHeader("New-Access-Token", newAccessToken);
            }
        }

        filterChain.doFilter(request, response);
    }
}
