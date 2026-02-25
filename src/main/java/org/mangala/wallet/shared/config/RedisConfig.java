package org.mangala.wallet.shared.config;

import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures a Redisson client used for distributed locking across service instances.
 *
 * <p>Reads the Redis address from {@code spring.data.redis.url} (default:
 * {@code redis://localhost:6379}). The address must use the {@code redis://} or
 * {@code rediss://} scheme as required by the Redisson single-server config.
 */
@Slf4j
@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.url:redis://localhost:6379}")
    private String redisUrl;

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient() {
        log.info("Initialising Redisson client for distributed locking, address={}", redisUrl);

        Config config = new Config();
        config.useSingleServer()
                .setAddress(redisUrl)
                .setConnectionMinimumIdleSize(2)
                .setConnectionPoolSize(10)
                .setConnectTimeout(3_000)
                .setTimeout(3_000)
                .setRetryAttempts(3)
                .setRetryInterval(500);

        return Redisson.create(config);
    }
}
