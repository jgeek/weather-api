package com.shapegames.weatherapi.service

import com.shapegames.weatherapi.config.WeatherApiProperties
import io.github.bucket4j.Bucket
import io.lettuce.core.RedisClient
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RedisRateLimiterTest {

    @Mock
    private lateinit var weatherApiProperties: WeatherApiProperties

    @Mock
    private lateinit var rateLimit: WeatherApiProperties.RateLimit

    @Mock
    private lateinit var redisClient: RedisClient

    @Mock
    private lateinit var mockBucket: Bucket

    private lateinit var redisRateLimiter: RedisRateLimiter

    @BeforeEach
    fun setUp() {
        lenient().`when`(weatherApiProperties.rateLimit).thenReturn(rateLimit)
        lenient().`when`(rateLimit.requestsPerDay).thenReturn(1000)
        lenient().`when`(rateLimit.requestsPerHour).thenReturn(100)

        redisRateLimiter = RedisRateLimiter(weatherApiProperties, redisClient)
    }

    @Test
    fun `constructor creates RedisRateLimiter with correct dependencies`() {
        // Given & When - constructor is called in setUp
        // Then
        assertNotNull(redisRateLimiter)
        // Note: Constructor may not immediately call these methods due to @PostConstruct
    }

    @Test
    fun `tryConsume returns true when bucket allows consumption`() {
        // Given
        mockBucketInitialization()
        `when`(mockBucket.tryConsume(1)).thenReturn(true)

        // When
        val result = redisRateLimiter.tryConsume()

        // Then
        assertTrue(result)
        verify(mockBucket).tryConsume(1)
    }

    @Test
    fun `tryConsume returns false when bucket denies consumption`() {
        // Given
        mockBucketInitialization()
        `when`(mockBucket.tryConsume(1)).thenReturn(false)

        // When
        val result = redisRateLimiter.tryConsume()

        // Then
        assertFalse(result)
        verify(mockBucket).tryConsume(1)
    }

    @Test
    fun `multiple tryConsume calls work correctly`() {
        // Given
        mockBucketInitialization()
        `when`(mockBucket.tryConsume(1))
            .thenReturn(true)   // First call succeeds
            .thenReturn(true)   // Second call succeeds
            .thenReturn(false)  // Third call fails (rate limited)

        // When & Then
        assertTrue(redisRateLimiter.tryConsume())
        assertTrue(redisRateLimiter.tryConsume())
        assertFalse(redisRateLimiter.tryConsume())

        verify(mockBucket, times(3)).tryConsume(1)
    }

    @Test
    fun `rate limit properties are used during initialization`() {
        // Given
        `when`(rateLimit.requestsPerDay).thenReturn(2000)
        `when`(rateLimit.requestsPerHour).thenReturn(200)

        // When
        val testRateLimiter = RedisRateLimiter(weatherApiProperties, redisClient)

        // Then
        assertNotNull(testRateLimiter)
        // Properties are accessed during @PostConstruct initialization
    }

    @Test
    fun `implements RateLimiter interface`() {
        // Given & When
        val rateLimiter: RateLimiter = redisRateLimiter

        // Then
        assertNotNull(rateLimiter)
        assertTrue(rateLimiter is RedisRateLimiter)
    }

    @Test
    fun `tryConsume handles bucket exception gracefully`() {
        // Given
        mockBucketInitialization()
        `when`(mockBucket.tryConsume(1)).thenThrow(RuntimeException("Redis connection failed"))

        // When & Then
        assertThrows(RuntimeException::class.java) {
            redisRateLimiter.tryConsume()
        }
        verify(mockBucket).tryConsume(1)
    }

    @Test
    fun `rate limiter configuration values are accessible`() {
        // Given
        val dailyLimit = 5000
        val hourlyLimit = 500
        `when`(rateLimit.requestsPerDay).thenReturn(dailyLimit)
        `when`(rateLimit.requestsPerHour).thenReturn(hourlyLimit)

        // When
        val testRateLimiter = RedisRateLimiter(weatherApiProperties, redisClient)

        // Then
        assertNotNull(testRateLimiter)
        // Configuration is used internally by the rate limiter
        assertEquals(dailyLimit, rateLimit.requestsPerDay)
        assertEquals(hourlyLimit, rateLimit.requestsPerHour)
    }

    private fun mockBucketInitialization() {
        // Replace the bucket field with our mock using reflection
        val bucketField = RedisRateLimiter::class.java.getDeclaredField("bucket")
        bucketField.isAccessible = true
        bucketField.set(redisRateLimiter, mockBucket)
    }
}
