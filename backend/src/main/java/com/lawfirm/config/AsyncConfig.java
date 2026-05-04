package com.lawfirm.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步配置
 * 支持异步处理耗时操作，提升系统并发能力
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    /**
     * 异步任务线程池
     * 核心线程数：10（支持常规并发）
     * 最大线程数：50（支持50人并发）
     * 队列容量：100（缓冲队列）
     */
    @Bean(name = "taskExecutor")
    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 核心线程数：即使在空闲时也保持存活的线程数
        executor.setCorePoolSize(10);

        // 最大线程数：线程池允许的最大线程数
        executor.setMaxPoolSize(50);

        // 队列容量：当核心线程都在运行时，新任务会进入队列等待
        executor.setQueueCapacity(100);

        // 线程名前缀
        executor.setThreadNamePrefix("async-task-");

        // 线程空闲时间（秒）：超过核心线程数的空闲线程在等待这么多秒后被销毁
        executor.setKeepAliveSeconds(60);

        // 拒绝策略：当队列满了且线程数达到最大值时的处理策略
        // CallerRunsPolicy：由调用线程（提交任务的线程）处理该任务
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        // 等待所有任务结束后再关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);

        // 等待时间（秒）：线程池关闭时最多等待任务完成的时间
        executor.setAwaitTerminationSeconds(60);

        executor.initialize();

        log.info("异步线程池初始化完成：核心线程数={}, 最大线程数={}, 队列容量={}",
                executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity());

        return executor;
    }

    /**
     * 异步异常处理器
     */
    @Bean(name = "asyncUncaughtExceptionHandler")
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (throwable, method, params) -> {
            log.error("异步任务执行异常 - 方法: {}, 参数: {}, 异常: {}",
                    method.getName(), params, throwable.getMessage(), throwable);
        };
    }
}
