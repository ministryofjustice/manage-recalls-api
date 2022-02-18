package uk.gov.justice.digital.hmpps.managerecallsapi.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.cache.CacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.RedisSerializer
import java.time.Duration

@Configuration
@ConditionalOnProperty(value = ["resolved.cache.enabled"], havingValue = "true", matchIfMissing = true)
class CacheConfiguration {
  @Value("\${resolved.cache.ttl:30}")
  private val ttlInMinutes: Long = 0

  @Bean(value = ["cacheManager"])
  fun redisCacheManager(lettuceConnectionFactory: LettuceConnectionFactory): CacheManager {
    val redisCacheConfiguration = RedisCacheConfiguration.defaultCacheConfig()
      .disableCachingNullValues()
      .entryTtl(Duration.ofMinutes(ttlInMinutes))
      .computePrefixWith { cacheName: String -> API_PREFIX + SEPARATOR + cacheName + SEPARATOR }
      .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(RedisSerializer.json()))
    redisCacheConfiguration.usePrefix()

    return RedisCacheManager.RedisCacheManagerBuilder.fromConnectionFactory(lettuceConnectionFactory)
      .cacheDefaults(redisCacheConfiguration).build()
  }

  companion object {
    private const val API_PREFIX = "apiCache"
    private const val SEPARATOR = ":"
  }
}
