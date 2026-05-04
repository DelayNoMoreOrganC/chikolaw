package com.lawfirm.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * 缓存配置
 * 性能优化：使用Caffeine高性能缓存支持50人并发
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Caffeine缓存管理器
     * Caffeine是高性能的Java缓存库，比Guava Cache性能更好
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();

        // 配置用户缓存（长期缓存）
        cacheManager.registerCustomCache("users", Caffeine.newBuilder()
                .maximumSize(100)  // 最多缓存100个用户
                .expireAfterWrite(30, TimeUnit.MINUTES)  // 30分钟过期
                .recordStats()  // 记录统计信息
                .build());

        // 配置角色缓存（长期缓存）
        cacheManager.registerCustomCache("roles", Caffeine.newBuilder()
                .maximumSize(50)  // 最多缓存50个角色
                .expireAfterWrite(1, TimeUnit.HOURS)  // 1小时过期
                .recordStats()
                .build());

        // 配置部门缓存（长期缓存）
        cacheManager.registerCustomCache("departments", Caffeine.newBuilder()
                .maximumSize(50)  // 最多缓存50个部门
                .expireAfterWrite(1, TimeUnit.HOURS)
                .recordStats()
                .build());

        // 配置系统配置缓存（长期缓存）
        cacheManager.registerCustomCache("systemConfig", Caffeine.newBuilder()
                .maximumSize(200)  // 最多缓存200个配置项
                .expireAfterWrite(1, TimeUnit.HOURS)
                .recordStats()
                .build());

        // 配置AI配置缓存（中期缓存）
        cacheManager.registerCustomCache("aiConfig", Caffeine.newBuilder()
                .maximumSize(50)  // 最多缓存50个AI配置
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .recordStats()
                .build());

        // 配置客户信息缓存（中期缓存）
        cacheManager.registerCustomCache("clients", Caffeine.newBuilder()
                .maximumSize(500)  // 最多缓存500个客户
                .expireAfterWrite(15, TimeUnit.MINUTES)
                .recordStats()
                .build());

        // 配置案件信息缓存（短期缓存）
        cacheManager.registerCustomCache("cases", Caffeine.newBuilder()
                .maximumSize(200)  // 最多缓存200个案件
                .expireAfterWrite(5, TimeUnit.MINUTES)  // 5分钟过期，因为案件状态经常变化
                .recordStats()
                .build());

        // 配置统计数据缓存（短期缓存）
        cacheManager.registerCustomCache("statistics", Caffeine.newBuilder()
                .maximumSize(100)  // 最多缓存100个统计结果
                .expireAfterWrite(2, TimeUnit.MINUTES)  // 2分钟过期
                .recordStats()
                .build());

        // 配置权限缓存（中期缓存）
        cacheManager.registerCustomCache("permissions", Caffeine.newBuilder()
                .maximumSize(100)  // 最多缓存100个权限集合
                .expireAfterWrite(20, TimeUnit.MINUTES)
                .recordStats()
                .build());

        return cacheManager;
    }
}
