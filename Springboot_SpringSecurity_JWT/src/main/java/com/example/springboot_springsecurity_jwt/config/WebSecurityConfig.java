package com.example.springboot_springsecurity_jwt.config;

import com.example.springboot_springsecurity_jwt.filter.TokenAuthenticationFilter;
import com.example.springboot_springsecurity_jwt.util.TokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 설정 클래스
 * 보안 관련 설정 및 JWT 기반 인증 필터 설정 정의
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class WebSecurityConfig {
    private final TokenProvider tokenProvider;

    // 비밀번호 암호화를 위한 PasswordEncoder 빈 등록
    // BCrypt 알고리즘 사용
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // JWT 인증 필터 등록
    @Bean
    public TokenAuthenticationFilter tokenAuthenticationFilter() {
        return new TokenAuthenticationFilter(tokenProvider);
    }

    // HTTP 보안 설정 정의
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CSRF 보호 비활성화 (JWT는 상태가 없으므로 CSRF 보호가 필요 없음)
                .csrf(csrf -> csrf.disable())

                // CORS 설정 추가
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // HTTP 요청에 대한 보안 설정 정의
                .authorizeHttpRequests(auth -> auth
                        // 다음 엔드포인트는 인증 없이 접근 허용
                        .requestMatchers("/api/member/signup").permitAll()
                        .requestMatchers("/api/member/login").permitAll()
                        // 그 외 모든 엔드포인트는 인증 필요
                        .anyRequest().authenticated()
                )

                // JWT 인증 필터를 UsernamePasswordAuthenticationFilter 앞에 추가
                .addFilterBefore(new TokenAuthenticationFilter(tokenProvider), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // CORS 설정 정의
    @Bean
    public org.springframework.web.cors.CorsConfigurationSource corsConfigurationSource() {
        // CORS 설정 객체 생성
        org.springframework.web.cors.CorsConfiguration configuration = new org.springframework.web.cors.CorsConfiguration();
        configuration.addAllowedOrigin("http://localhost");
        configuration.addAllowedOrigin("http://localhost:3000");
        configuration.addAllowedMethod("*");
        configuration.addAllowedHeader("*");

        // 특정 URL 패턴에 대해 CORS 설정 등록
        org.springframework.web.cors.UrlBasedCorsConfigurationSource source = new org.springframework.web.cors.UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}
