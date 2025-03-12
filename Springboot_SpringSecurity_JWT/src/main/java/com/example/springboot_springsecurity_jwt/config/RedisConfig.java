package com.example.springboot_springsecurity_jwt.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.CacheKeyPrefix;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import java.time.Duration;

/**
 * Redis 설정 클래스
 * Redis 연결 및 캐시 관리를 위한 설정을 정의함.
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
                .commandTimeout(Duration.ZERO) // 명령 시간 초과 설정 (0: 무제한)
                .shutdownTimeout(Duration.ZERO) // 종료 시간 초과 설정 (0: 무제한)
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

    /**
     * CacheManager 빈 등록
     * Redis를 캐시 저장소로 사용하는 CacheManager 설정
     */
    @Bean
    public CacheManager cacheManager(final RedisConnectionFactory redisConnectionFactory) {
        // Redis 캐시 설정 정의
        final RedisCacheConfiguration configuration = RedisCacheConfiguration.defaultCacheConfig()
                // 키 직렬화 설정
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                // 값 직렬화 설정
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()))
                // 캐시 만료 시간 설정 (2분)
                .entryTtl(Duration.ofMinutes(2))
                // null 값 캐싱 방지
                .disableCachingNullValues()
                // 캐시 키에 접두사 설정
                .computePrefixWith(CacheKeyPrefix.simple());

        // RedisCacheManager 생성 및 설정 적용
        return RedisCacheManager.RedisCacheManagerBuilder
                .fromConnectionFactory(redisConnectionFactory)
                .cacheDefaults(configuration)
                .build();
    }
}
