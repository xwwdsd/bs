package com.cs2trade.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 异步配置类
 * 配置异步任务执行的线程池
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean("steamSyncExecutor")
    public Executor steamSyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(10);
        executor.setThreadNamePrefix("steam-sync-");
        executor.setRejectedExecutionHandler((r, e) -> {
            log.warn("同步任务被拒绝，线程池已满");
        });
        executor.initialize();
        log.info("Steam 同步异步线程池初始化完成");
        return executor;
    }
}
