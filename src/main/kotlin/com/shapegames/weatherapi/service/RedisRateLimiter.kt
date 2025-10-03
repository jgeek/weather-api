package com.shapegames.weatherapi.service

import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import io.github.bucket4j.BucketConfiguration
import io.github.bucket4j.Refill
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager
import io.lettuce.core.RedisClient
import com.shapegames.weatherapi.config.WeatherApiProperties
import org.springframework.stereotype.Service
import java.time.Duration
import java.util.function.Supplier
import jakarta.annotation.PostConstruct

@Service
class RedisRateLimiter(
    private val weatherApiProperties: WeatherApiProperties,
    private val redisClient: RedisClient
) : RateLimiter {

    private lateinit var bucket: Bucket

    @PostConstruct
    private fun init() {
        val proxyManager = createProxyManager()
        val bucketConfigSupplier = createBucketConfigSupplier()
        bucket = proxyManager.builder().build("OpenWeatherMap-API-limit".toByteArray(), bucketConfigSupplier)
    }

    private fun createProxyManager(): LettuceBasedProxyManager {
        return LettuceBasedProxyManager.builderFor(redisClient)
            .withExpirationStrategy(
                ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(Duration.ofSeconds(10))
            )
            .build()
    }

    private fun createBucketConfigSupplier(): Supplier<BucketConfiguration> {
        return Supplier {
            BucketConfiguration.builder()
                .addLimit(createDailyLimit())
                .addLimit(createHourlyLimit())
                .build()
        }
    }

    private fun createDailyLimit(): Bandwidth {
        return Bandwidth.simple(
            weatherApiProperties.rateLimit.requestsPerDay.toLong(),
            Duration.ofDays(1)
        )
    }

    private fun createHourlyLimit(): Bandwidth {
        return Bandwidth.classic(
            weatherApiProperties.rateLimit.requestsPerHour.toLong(),
            Refill.greedy(
                weatherApiProperties.rateLimit.requestsPerHour.toLong(),
                Duration.ofHours(1)
            )
        )
    }

    override fun tryConsume(): Boolean = bucket.tryConsume(1)
}
