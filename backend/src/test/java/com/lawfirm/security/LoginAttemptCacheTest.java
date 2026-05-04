package com.lawfirm.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * LoginAttemptCache 线程安全测试
 */
public class LoginAttemptCacheTest {

    private LoginAttemptCache cache;

    @BeforeEach
    public void setUp() {
        cache = new LoginAttemptCache();
    }

    /**
     * 测试1：验证线程安全 - 使用AtomicInteger
     */
    @Test
    public void testThreadSafety() throws InterruptedException {
        String username = "test_user";
        
        // 多线程并发增加失败次数
        Thread[] threads = new Thread[10];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 10; j++) {
                    cache.recordFailedAttempt(username);
                }
            });
            threads[i].start();
        }
        
        // 等待所有线程完成
        for (Thread thread : threads) {
            thread.join();
        }
        
        // 验证最终次数正确（10个线程 * 10次 = 100次）
        Integer count = cache.getFailedAttempts(username);
        assertEquals(100, count, "并发情况下失败次数应该正确累加");
        
        System.out.println("✓ 线程安全测试通过：AtomicInteger正确处理并发");
    }

    /**
     * 测试2：验证登录锁定逻辑
     */
    @Test
    public void testLockMechanism() {
        String username = "lock_test";
        
        // 连续5次失败
        for (int i = 0; i < 5; i++) {
            cache.recordFailedAttempt(username);
        }
        
        // 验证被锁定
        assertTrue(cache.isLocked(username), "5次失败后应该被锁定");
        
        // 验证剩余时间
        Long remaining = cache.getRemainingLockTime(username);
        assertTrue(remaining > 0 && remaining <= 30, "剩余锁定时间应该在0-30分钟之间");
        
        System.out.println("✓ 锁定逻辑测试通过：5次失败后正确锁定");
    }

    /**
     * 测试3：验证清除功能
     */
    @Test
    public void testClearAttempts() {
        String username = "clear_test";
        
        // 制造失败记录
        cache.recordFailedAttempt(username);
        cache.recordFailedAttempt(username);
        assertEquals(2, cache.getFailedAttempts(username));
        
        // 清除记录
        cache.clearFailedAttempts(username);
        assertEquals(0, cache.getFailedAttempts(username), "清除后失败次数应该为0");
        assertFalse(cache.isLocked(username), "清除后不应该被锁定");
        
        System.out.println("✓ 清除功能测试通过");
    }
}
