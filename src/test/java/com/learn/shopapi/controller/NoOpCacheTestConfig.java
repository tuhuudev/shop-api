package com.learn.shopapi.controller;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.context.annotation.Bean;

/**
 * @EnableCaching tren ShopApiApplication van duoc kich hoat trong @WebMvcTest slice,
 * nhung RedisConfig (cung CacheManager) khong duoc nap -> cap CacheManager khong lam gi
 * de context khoi tao duoc ma khong cham Redis.
 */
@TestConfiguration(proxyBeanMethods = false)
public class NoOpCacheTestConfig {

    @Bean
    public CacheManager cacheManager() {
        return new NoOpCacheManager();
    }
}
