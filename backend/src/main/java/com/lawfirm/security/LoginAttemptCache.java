package com.lawfirm.security;

import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 登录失败记录器（内存存储）
 * 用于Redis不可用时的备用方案
 */
@Slf4j
@Component
public class LoginAttemptCache {

    private final Map<String, LoginAttempt> attempts = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public LoginAttemptCache() {
        // 定期清理过期的记录
        scheduler.scheduleAtFixedRate(this::cleanExpiredAttempts, 1, 1, TimeUnit.HOURS);
    }

    /**
     * 记录登录失败
     */
    public void recordFailedAttempt(String username) {
        String key = "login:fail:" + username;

        LoginAttempt attempt = attempts.compute(key, (k, v) -> {
            if (v == null) {
                return new LoginAttempt();
            }
            v.increment();
            return v;
        });

        log.warn("用户登录失败: {}, 失败次数: {}, 锁定状态: {}, cache总条数={}",
            username, attempt.getCount(), attempt.isLocked(), attempts.size());
    }

    /**
     * 获取失败次数
     */
    public Integer getFailedAttempts(String username) {
        String key = "login:fail:" + username;
        LoginAttempt attempt = attempts.get(key);
        int count = attempt != null ? attempt.getCount() : 0;
        log.info("获取失败次数 - key={}, count={}, cache大小={}", key, count, attempts.size());
        return count;
    }

    /**
     * 检查是否被锁定
     */
    public boolean isLocked(String username) {
        String key = "login:fail:" + username;
        LoginAttempt attempt = attempts.get(key);
        return attempt != null && attempt.isLocked();
    }

    /**
     * 获取剩余锁定时间（分钟）
     */
    public Long getRemainingLockTime(String username) {
        String key = "login:fail:" + username;
        LoginAttempt attempt = attempts.get(key);
        if (attempt != null && attempt.isLocked()) {
            long remaining = attempt.getLockEndTime() - System.currentTimeMillis();
            return TimeUnit.MILLISECONDS.toMinutes(remaining);
        }
        return 0L;
    }

    /**
     * 清除失败记录
     */
    public void clearFailedAttempts(String username) {
        String key = "login:fail:" + username;
        attempts.remove(key);
        log.info("清除登录失败记录: {}", username);
    }

    /**
     * 清理过期的记录
     */
    private void cleanExpiredAttempts() {
        long now = System.currentTimeMillis();
        attempts.entrySet().removeIf(entry -> {
            LoginAttempt attempt = entry.getValue();
            if (attempt.getLockEndTime() > 0 && attempt.getLockEndTime() < now) {
                log.info("清理过期的登录锁定记录: {}", entry.getKey());
                return true;
            }
            // 清理超过24小时的非锁定记录
            if (attempt.getLockEndTime() == 0 &&
                (now - attempt.getFirstFailTime()) > TimeUnit.HOURS.toMillis(24)) {
                log.info("清理过期的登录失败记录: {}", entry.getKey());
                return true;
            }
            return false;
        });
    }

    /**
     * 登录尝试记录
     */
    public static class LoginAttempt {
        private final java.util.concurrent.atomic.AtomicInteger count = new java.util.concurrent.atomic.AtomicInteger(1);
        private long firstFailTime = System.currentTimeMillis();
        private long lockEndTime;

        public void increment() {
            int newCount = this.count.incrementAndGet();
            // 达到5次失败时锁定30分钟
            if (newCount >= 5 && lockEndTime == 0) {
                synchronized (this) {
                    // 双重检查锁定
                    if (lockEndTime == 0) {
                        this.lockEndTime = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(30);
                    }
                }
            }
        }

        public int getCount() {
            return count.get();
        }

        public boolean isLocked() {
            return lockEndTime > 0 && System.currentTimeMillis() < lockEndTime;
        }

        public long getLockEndTime() {
            return lockEndTime;
        }

        public long getFirstFailTime() {
            return firstFailTime;
        }
    }
}
