package devut.buzzerbidder.global.config;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "notificationExecutor")
    public Executor notificationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 기본 스레드 풀 크기 (항상 유지되는 스레드 수)
        executor.setCorePoolSize(2);

        // 최대 스레드 풀 크기 (부하 발생시 증가 가능한 최대값)
        executor.setMaxPoolSize(5);

        // 큐 용량 (스레드가 모두 사용중일 때 대기하는 작업 수)
        executor.setQueueCapacity(50);

        // 스레드 이름 접두사
        executor.setThreadNamePrefix("notification-async-");

        // 큐가 꽉 찼을 때 정책 : 호출한 스레드에서 직접 실행
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        // 종료시 남은 작업 완료 대기
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);

        executor.initialize();

        log.info("Notification Executor initialized - corePoolSize: {}, maxPoolSize: {}, queueCapacity: {}",
            executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity());

        return executor;
    }

}
