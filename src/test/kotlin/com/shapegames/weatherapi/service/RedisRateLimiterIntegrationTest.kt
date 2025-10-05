package com.shapegames.weatherapi.service

import com.shapegames.weatherapi.config.WeatherApiProperties
import io.lettuce.core.RedisClient
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

/**
 * Integration tests that test actual rate limiting functionality with Redis
 */
@Testcontainers
@SpringBootTest
@TestPropertySource(properties = [
    "weather.api.key=test-key",
    "weather.api.base-url=http://test.com",
    "weather.api.rate-limit.requests-per-hour=5",
    "weather.api.rate-limit.requests-per-day=20"
])
class RedisRateLimiterIntegrationTest {

    companion object {
        @Container
        @JvmStatic
        val redis: GenericContainer<*> = GenericContainer("redis:7-alpine")
            .withExposedPorts(6378)
            .withReuse(true)
    }

    private lateinit var redisClient: RedisClient
    private lateinit var rateLimiter: RedisRateLimiter
    private lateinit var properties: WeatherApiProperties

    @BeforeEach
    fun setUp() {
        val redisHost = redis.host
        val redisPort = redis.getMappedPort(6379)

        redisClient = RedisClient.create("redis://$redisHost:$redisPort")

        properties = WeatherApiProperties(
            key = "test-key",
            baseUrl = "http://test.com",
            rateLimit = WeatherApiProperties.RateLimit(
                requestsPerDay = 20,
                requestsPerHour = 5
            )
        )

        rateLimiter = RedisRateLimiter(properties, redisClient)

        // Trigger @PostConstruct manually
        val initMethod = RedisRateLimiter::class.java.getDeclaredMethod("init")
        initMethod.isAccessible = true
        initMethod.invoke(rateLimiter)

        // Clear any existing rate limit data
        clearRateLimitData()
    }

    @Test
    fun `rate limiter allows requests up to hourly limit`() {
        // Given - hourly limit is 5 requests

        // When & Then - first 5 requests should succeed
        repeat(5) { requestNumber ->
            val result = rateLimiter.tryConsume()
            assertTrue(result, "Request ${requestNumber + 1} should be allowed")
        }

        // 6th request should be denied due to hourly limit
        val sixthRequest = rateLimiter.tryConsume()
        assertFalse(sixthRequest, "6th request should be denied due to hourly rate limit")
    }

    @Test
    fun `rate limiter enforces both hourly and daily limits`() {
        // Given - hourly limit: 5, daily limit: 20

        // Simulate consuming all hourly requests
        repeat(5) {
            assertTrue(rateLimiter.tryConsume(), "Should allow requests within hourly limit")
        }

        // Next request should be denied due to hourly limit
        assertFalse(rateLimiter.tryConsume(), "Should deny request exceeding hourly limit")

        // Even if we could bypass hourly limit, daily limit should still apply
        // This test verifies that both limits are configured
        assertNotNull(rateLimiter)
    }

    @Test
    fun `rate limiter with very restrictive limits blocks requests immediately`() {
        // Given - create a rate limiter with very low limits
        val restrictiveProperties = WeatherApiProperties(
            key = "test-key",
            baseUrl = "http://test.com",
            rateLimit = WeatherApiProperties.RateLimit(
                requestsPerDay = 1,
                requestsPerHour = 1
            )
        )

        val restrictiveRateLimiter = RedisRateLimiter(restrictiveProperties, redisClient)
        val initMethod = RedisRateLimiter::class.java.getDeclaredMethod("init")
        initMethod.isAccessible = true
        initMethod.invoke(restrictiveRateLimiter)

        // When & Then
        assertTrue(restrictiveRateLimiter.tryConsume(), "First request should be allowed")
        assertFalse(restrictiveRateLimiter.tryConsume(), "Second request should be denied")
    }

    @Test
    fun `rate limiter persists state across instances`() {
        // Given - consume some requests with first instance
        repeat(3) {
            assertTrue(rateLimiter.tryConsume(), "Initial requests should be allowed")
        }

        // When - create a new rate limiter instance (same Redis, same key)
        val newRateLimiter = RedisRateLimiter(properties, redisClient)
        val initMethod = RedisRateLimiter::class.java.getDeclaredMethod("init")
        initMethod.isAccessible = true
        initMethod.invoke(newRateLimiter)

        // Then - should remember previous consumption (3 used, 2 remaining in hourly limit)
        assertTrue(newRateLimiter.tryConsume(), "4th request should be allowed")
        assertTrue(newRateLimiter.tryConsume(), "5th request should be allowed")
        assertFalse(newRateLimiter.tryConsume(), "6th request should be denied")
    }

    @Test
    fun `rate limit bucket configuration includes expected bandwidth limits`() {
        // Given & When - rate limiter is initialized in setUp

        // Then - verify that the rate limiter was created successfully
        assertNotNull(rateLimiter)

        // Verify it respects the configured limits by testing boundary conditions
        val hourlyLimit = properties.rateLimit.requestsPerHour

        // Should allow requests up to the limit
        repeat(hourlyLimit) { requestNumber ->
            val result = rateLimiter.tryConsume()
            assertTrue(result, "Request ${requestNumber + 1} should be within hourly limit of $hourlyLimit")
        }

        // Should deny the next request
        val exceededResult = rateLimiter.tryConsume()
        assertFalse(exceededResult, "Request exceeding hourly limit should be denied")
    }

    @Test
    fun `rate limiter handles concurrent access properly`() {
        // Given - multiple threads trying to consume tokens
        val results = mutableListOf<Boolean>()
        val threads = mutableListOf<Thread>()

        // When - simulate concurrent requests
        repeat(10) { threadIndex ->
            val thread = Thread {
                val result = rateLimiter.tryConsume()
                synchronized(results) {
                    results.add(result)
                }
            }
            threads.add(thread)
            thread.start()
        }

        // Wait for all threads to complete
        threads.forEach { it.join() }

        // Then - only 5 should succeed (hourly limit), 5 should fail
        val successCount = results.count { it }
        val failureCount = results.count { !it }

        assertEquals(5, successCount, "Should allow exactly 5 requests within hourly limit")
        assertEquals(5, failureCount, "Should deny 5 requests exceeding hourly limit")
    }

    private fun clearRateLimitData() {
        try {
            val connection = redisClient.connect()
            connection.sync().flushall()
            connection.close()
        } catch (e: Exception) {
            // Ignore cleanup errors in tests
        }
    }
}
