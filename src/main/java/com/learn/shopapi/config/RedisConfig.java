package com.learn.shopapi.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Duration;

/**
 * Cau hinh Redis: cache manager (@Cacheable) + cac RedisTemplate.
 * StringRedisTemplate (rate-limit, login-attempts) da co san tu Spring Boot.
 */
@Configuration
public class RedisConfig {

    /**
     * Serializer JSON cho Redis: JavaTimeModule (xu ly LocalDateTime/Instant) + type info (@class)
     * de deserialize dung kieu (ke ca record). PTV cho phep moi kieu - chap nhan vi day la cache noi bo.
     */
    private GenericJackson2JsonRedisSerializer jsonSerializer() {
        ObjectMapper om = new ObjectMapper();
        om.registerModule(new JavaTimeModule());
        om.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        om.activateDefaultTyping(
                BasicPolymorphicTypeValidator.builder().allowIfBaseType(Object.class).build(),
                ObjectMapper.DefaultTyping.EVERYTHING, JsonTypeInfo.As.PROPERTY);
        return new GenericJackson2JsonRedisSerializer(om);
    }

    /** Cache value qua JSON, TTL 10 phut (vd "categories"). */
    @Bean
    public RedisCacheConfiguration cacheConfiguration() {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .disableCachingNullValues()
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer()));
    }

    /** Cache UserDetails: key chuoi, value JDK (lop User cua Spring Security la Serializable). */
    @Bean
    public RedisTemplate<String, UserDetails> userDetailsRedisTemplate(RedisConnectionFactory cf) {
        RedisTemplate<String, UserDetails> template = new RedisTemplate<>();
        template.setConnectionFactory(cf);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(RedisSerializer.java());
        return template;
    }

    /** Template JSON dung chung (vd luu ket qua idempotency) - DTO record/co LocalDateTime deu OK. */
    @Bean
    public RedisTemplate<String, Object> jsonRedisTemplate(RedisConnectionFactory cf) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(cf);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(jsonSerializer());
        return template;
    }
}
