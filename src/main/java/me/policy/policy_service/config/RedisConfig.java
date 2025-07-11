package me.policy.policy_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableConfigurationProperties   // lets Spring read application.properties
public class RedisConfig {

    @Bean
    public LettuceConnectionFactory redisConnectionFactory(
            @Value("${spring.data.redis.cluster.nodes}") String nodes,
            @Value("${spring.data.redis.password:}") String password   // empty if not set
    ) {
        List<String> nodeList = Arrays.asList(nodes.split("\\s*,\\s*"));
        RedisClusterConfiguration cfg = new RedisClusterConfiguration(nodeList);

        if (StringUtils.hasText(password)) {
            cfg.setPassword(password);
        }
        return new LettuceConnectionFactory(cfg);
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(LettuceConnectionFactory cf) {
        RedisTemplate<String, Object> tpl = new RedisTemplate<>();
        tpl.setConnectionFactory(cf);
        tpl.setKeySerializer(new StringRedisSerializer());
        tpl.setValueSerializer(new StringRedisSerializer());
        return tpl;
    }
}
