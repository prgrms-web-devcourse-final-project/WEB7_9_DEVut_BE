package devut.buzzerbidder.global.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "spring.data.redis.host")
public class RedissonConfig {

    @Value("${spring.data.redis.host}")
    private String host;

    @Value("${spring.data.redis.port}")
    private int port;

    @Value("${spring.data.redis.password}")
    private String password;

    private static final String REDISSON_HOST_PREFIX = "redis://";

    @Bean
    public RedissonClient redissonClient(){
        Config config = new Config();
        var serverConfig = config.useSingleServer().setAddress(REDISSON_HOST_PREFIX + host + ":" + port);

        // password가 비어있지 않을 때만 설정
        if (password != null && !password.isEmpty()) {
            serverConfig.setPassword(password);
        }


        return Redisson.create(config);
    }

}
