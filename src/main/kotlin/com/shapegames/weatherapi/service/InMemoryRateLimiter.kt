package com.shapegames.weatherapi.service

import com.shapegames.weatherapi.config.WeatherApiProperties
import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import io.github.bucket4j.Refill
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import java.time.Duration
import jakarta.annotation.PostConstruct

/**
 * In-memory rate limiter implementation for testing purposes.
 * Uses local buckets instead of Redis for faster test execution.
 */
@Service
@Profile("test")
class InMemoryRateLimiter(
    private val weatherApiProperties: WeatherApiProperties
) : RateLimiter {

    private lateinit var bucket: Bucket

    @PostConstruct
    private fun init() {
        bucket = createBucket()
    }

    private fun createBucket(): Bucket {
        return Bucket.builder()
            .addLimit(createDailyLimit())
            .addLimit(createHourlyLimit())
            .build()
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
            Refill.intervally(
                weatherApiProperties.rateLimit.requestsPerHour.toLong(),
                Duration.ofHours(1)
            )
        )
    }

    override fun tryConsume(): Boolean {
        return bucket.tryConsume(1)
    }

    /**
     * Reset the bucket for testing purposes
     */
    fun reset() {
        bucket = createBucket()
    }

    /**
     * Get available tokens for testing purposes
     */
    fun getAvailableTokens(): Long {
        return bucket.availableTokens
    }
}
