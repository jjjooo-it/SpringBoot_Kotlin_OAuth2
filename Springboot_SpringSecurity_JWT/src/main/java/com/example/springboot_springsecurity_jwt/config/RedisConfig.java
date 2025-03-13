package com.example.springboot_springsecurity_jwt.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Redis 설정 클래스
 * Redis 연결을 위한 설정을 정의하고 redisTemplate를 빈으로 등록
 */
@Configuration
public class RedisConfig {

    // application.yml 에서 Redis 설정 값 주입
    @Value(value = "${spring.data.redis.host}")
    private String host;

    @Value(value = "${spring.data.redis.port}")
    private int port;

    /**
     * RedisConnectionFactory 빈 등록
     * Redis에 연결하기 위한 팩토리 객체 생성
     */
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        // RedisStandaloneConfiguration: 단일 노드 Redis 구성 설정
        final RedisStandaloneConfiguration standaloneConfiguration = new RedisStandaloneConfiguration(host, port);

        // LettuceClientConfiguration: Lettuce 클라이언트 구성 설정
        final LettuceClientConfiguration clientConfiguration = LettuceClientConfiguration.builder()
                .commandTimeout(Duration.ZERO)
                .shutdownTimeout(Duration.ZERO)
                .build();

        // LettuceConnectionFactory: Lettuce 기반 Redis 연결 팩토리 생성
        return new LettuceConnectionFactory(standaloneConfiguration, clientConfiguration);
    }

    /**
     * RedisTemplate 빈 등록
     * Redis에서 데이터를 읽고 쓰기 위한 템플릿 설정
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new StringRedisSerializer());
        return redisTemplate;
    }
}
