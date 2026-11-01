package dev.portfolio.threatlens;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Duration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

@Configuration
@EnableCaching
public class CacheConfiguration {
    @Bean
    RedisCacheConfiguration redisCacheConfiguration() {
	BasicPolymorphicTypeValidator validator = BasicPolymorphicTypeValidator.builder()
		.allowIfSubType("dev.portfolio.threatlens.")
		.allowIfSubType("java.util.")
		.allowIfSubType("java.time.")
		.build();
	ObjectMapper mapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();
	mapper.activateDefaultTyping(validator, ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
	return RedisCacheConfiguration.defaultCacheConfig()
		.entryTtl(Duration.ofSeconds(30))
		.computePrefixWith(cacheName -> "threatlens:" + cacheName + ":")
		.serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(
			new GenericJackson2JsonRedisSerializer(mapper)))
		.disableCachingNullValues();
    }
}