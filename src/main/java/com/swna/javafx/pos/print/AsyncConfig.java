package com.swna.javafx.pos.print;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync // 중요: 비동기 기능을 활성화합니다.
public class AsyncConfig {

    @Bean(name = "printExecutor") // 리스너에서 찾는 그 이름입니다.
    public Executor printExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // 포스기 환경에 맞는 설정
        executor.setCorePoolSize(2);        // 기본적으로 유지할 쓰레드 수
        executor.setMaxPoolSize(5);         // 최대 생성 가능한 쓰레드 수
        executor.setQueueCapacity(100);     // 쓰레드가 꽉 찼을 때 대기할 작업 수
        executor.setThreadNamePrefix("ReceiptPrint-"); // 로그에서 확인할 이름
        
        executor.initialize();
        return executor;
    }
}