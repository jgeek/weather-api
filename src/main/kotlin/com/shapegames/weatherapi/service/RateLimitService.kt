package com.shapegames.weatherapi.service

import com.shapegames.weatherapi.config.WeatherApiProperties
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class RateLimitService(
    private val redisTemplate: RedisTemplate<String, String>,
    private val weatherApiProperties: WeatherApiProperties
) {

    companion object {
        private const val RATE_LIMIT_KEY = "weather_api_rate_limit"
        private const val DAILY_KEY_FORMAT = "weather_api_requests:%s"
    }

    fun canMakeRequest(): Boolean {
        val today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val key = DAILY_KEY_FORMAT.format(today)

        val currentCount = redisTemplate.opsForValue().get(key)?.toIntOrNull() ?: 0

        return currentCount < weatherApiProperties.rateLimit.requestsPerDay
    }

    fun incrementRequestCount(): Boolean {
        val today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val key = DAILY_KEY_FORMAT.format(today)

        val newCount = redisTemplate.opsForValue().increment(key) ?: 1

        // Set expiration for the key (24 hours)
        redisTemplate.expire(key, Duration.ofHours(24))

        return newCount <= weatherApiProperties.rateLimit.requestsPerDay
    }

    fun getRemainingRequests(): Int {
        val today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val key = DAILY_KEY_FORMAT.format(today)

        val currentCount = redisTemplate.opsForValue().get(key)?.toIntOrNull() ?: 0
        return maxOf(0, weatherApiProperties.rateLimit.requestsPerDay - currentCount)
    }
}
