package com.lyj.dada.config;


import io.reactivex.Scheduler;
import io.reactivex.schedulers.Schedulers;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 创建隔离线程池
 */
@Configuration
@Data
public class VipSchedulerConfig {

    /**
     * 创建线程
     * @return
     */
    @Bean
    public Scheduler vipScheduler() {
        ThreadFactory threadFactory = new ThreadFactory() {
            private final AtomicInteger threadNumber = new AtomicInteger(1);
            
            @Override
            public Thread newThread(@NotNull Runnable r) {
                Thread thread = new Thread(r, "VIPThreadPool-" + threadNumber.getAndIncrement());
                // 非守护线程
                thread.setDaemon(false);
                return thread;
            }
        };
        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(10, threadFactory);
        //
        return Schedulers.from(scheduledExecutorService);
    }
}
