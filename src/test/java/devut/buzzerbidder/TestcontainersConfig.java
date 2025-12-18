package devut.buzzerbidder;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.TimeoutOptions;
import java.time.Duration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;
import org.mockito.Mockito;

/**
 * Testcontainers 설정 클래스
 * 테스트 실행 시 MySQL과 Redis 컨테이너를 자동으로 시작하고 종료합니다.
 * 
 * 사용법:
 * - 각 테스트 클래스에 @Import(TestcontainersConfig.class) 추가
 * - @ServiceConnection을 사용하여 MySQL 데이터소스 속성이 자동으로 설정됩니다.
 * - Redis는 testRedisConnectionFactory Bean을 통해 자동으로 연결됩니다.
 * 
 * 참고: 
 * - @ServiceConnection은 Spring Boot 3.1+에서 제공하는 기능으로,
 *   Testcontainers와 Spring Boot를 자동으로 연결해줍니다.
 * - Redis ConnectionFactory는 @Lazy로 설정하여 컨테이너가 준비된 후에 생성되도록 합니다.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfig {

    // 컨테이너 선언 - static으로 선언하여 클래스 단위로 컨테이너 생성/종료
    @Container
    static final MySQLContainer<?> mysql = new MySQLContainer<>(DockerImageName.parse("mysql:8.0.33"))
            .withDatabaseName("test_db")
            .withUsername("test")
            .withPassword("test");

    @Container
    static final GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    // 컨테이너 실행 - static 초기화 블록
    // @DynamicPropertySource가 실행되기 전에 컨테이너가 시작되어야 함
    static {
        mysql.start();
        redis.start();
    }

    /**
     * MySQL 컨테이너 인스턴스 접근용 메서드
     */
    public static MySQLContainer<?> getMysql() {
        return mysql;
    }

    /**
     * Redis 컨테이너 인스턴스 접근용 메서드
     */
    public static GenericContainer<?> getRedis() {
        return redis;
    }

    /**
     * MySQL 컨테이너를 Bean으로 등록
     * @ServiceConnection을 사용하여 Spring Boot가 자동으로 데이터소스 속성을 설정합니다.
     */
    @Bean
    @ServiceConnection
    MySQLContainer<?> mysqlContainer() {
        return mysql;
    }

    /**
     * Redis 컨테이너를 Bean으로 등록
     */
    @Bean
    GenericContainer<?> redisContainer() {
        return redis;
    }

    /**
     * 테스트용 Redis ConnectionFactory Bean
     * @Lazy로 설정하여 컨테이너가 준비된 후에 생성되도록 합니다.
     * @Primary로 설정하여 테스트 환경에서 기본으로 사용됩니다.
     * 재연결을 비활성화하여 테스트 종료 시 무한 재연결을 방지합니다.
     */
    @Bean
    @Primary
    @Lazy
    RedisConnectionFactory testRedisConnectionFactory() {
        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration();
        redisConfig.setHostName(redis.getHost());
        redisConfig.setPort(redis.getMappedPort(6379));
        
        // Lettuce ClientOptions 설정 - 재연결 비활성화
        ClientOptions clientOptions = ClientOptions.builder()
                .autoReconnect(false)  // 자동 재연결 비활성화
                .timeoutOptions(TimeoutOptions.builder()
                        .fixedTimeout(Duration.ofSeconds(2))
                        .build())
                .build();
        
        // LettuceClientConfiguration 생성
        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                .clientOptions(clientOptions)
                .commandTimeout(Duration.ofSeconds(2))
                .build();
        
        // LettuceConnectionFactory 생성
        LettuceConnectionFactory factory = new LettuceConnectionFactory(redisConfig, clientConfig);
        factory.afterPropertiesSet();
        return factory;
    }

    /**
     * 테스트용 JavaMailSender Mock Bean
     * 테스트 환경에서 실제 메일을 보낼 필요가 없으므로 Mock으로 제공합니다.
     */
    @Bean
    @Primary
    JavaMailSender testJavaMailSender() {
        return Mockito.mock(JavaMailSender.class);
    }
}
