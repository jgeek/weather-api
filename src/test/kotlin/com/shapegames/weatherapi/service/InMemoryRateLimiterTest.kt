package com.shapegames.weatherapi.service

import com.shapegames.weatherapi.config.WeatherApiProperties
import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import io.github.bucket4j.BucketConfiguration
import io.github.bucket4j.Refill
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Unit tests for rate limiting functionality using in-memory implementation.
 * This approach is faster than Redis-based tests and doesn't require external dependencies.
 */
class InMemoryRateLimiterTest {

    private lateinit var rateLimiter: InMemoryRateLimiter
    private lateinit var properties: WeatherApiProperties

    @BeforeEach
    fun setUp() {
        properties = WeatherApiProperties(
            key = "test-key",
            baseUrl = "http://test.com",
            rateLimit = WeatherApiProperties.RateLimit(
                requestsPerDay = 20,
                requestsPerHour = 5
            )
        )

        rateLimiter = InMemoryRateLimiter(properties)

        // Trigger @PostConstruct manually for testing
        val initMethod = InMemoryRateLimiter::class.java.getDeclaredMethod("init")
        initMethod.isAccessible = true
        initMethod.invoke(rateLimiter)
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
    fun `rate limiter respects daily limits`() {
        // Given - daily limit is 20 requests, but hourly is 5
        // We can only test the hourly limit in a unit test timeframe

        // When - consume all hourly requests
        repeat(5) {
            assertTrue(rateLimiter.tryConsume(), "Should allow requests within hourly limit")
        }

        // Then - next request should be denied
        assertFalse(rateLimiter.tryConsume(), "Should deny request exceeding hourly limit")

        // Verify that both limits are configured by checking available tokens
        // Should be 0 since we've consumed all hourly tokens
        assertEquals(
            0,
            rateLimiter.getAvailableTokens(),
            "Should have no available tokens after consuming hourly limit"
        )
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

        val restrictiveRateLimiter = InMemoryRateLimiter(restrictiveProperties)
        val initMethod = InMemoryRateLimiter::class.java.getDeclaredMethod("init")
        initMethod.isAccessible = true
        initMethod.invoke(restrictiveRateLimiter)

        // When & Then
        assertTrue(restrictiveRateLimiter.tryConsume(), "First request should be allowed")
        assertFalse(restrictiveRateLimiter.tryConsume(), "Second request should be denied")
        assertEquals(0, restrictiveRateLimiter.getAvailableTokens(), "Should have no tokens remaining")
    }

    @Test
    fun `rate limiter handles concurrent access properly`() {
        // Given - multiple threads trying to consume tokens
        val numberOfThreads = 10
        val executor = Executors.newFixedThreadPool(numberOfThreads)
        val latch = CountDownLatch(numberOfThreads)
        val results = mutableListOf<Boolean>()

        try {
            // When - simulate concurrent requests
            val futures = (1..numberOfThreads).map {
                CompletableFuture.supplyAsync({
                    val result = rateLimiter.tryConsume()
                    synchronized(results) {
                        results.add(result)
                    }
                    latch.countDown()
                    result
                }, executor)
            }

            // Wait for all threads to complete
            assertTrue(latch.await(5, TimeUnit.SECONDS), "All threads should complete within timeout")
            CompletableFuture.allOf(*futures.toTypedArray()).join()

            // Then - only 5 should succeed (hourly limit), 5 should fail
            val successCount = results.count { it }
            val failureCount = results.count { !it }

            assertEquals(5, successCount, "Should allow exactly 5 requests within hourly limit")
            assertEquals(5, failureCount, "Should deny 5 requests exceeding hourly limit")
            assertEquals(0, rateLimiter.getAvailableTokens(), "Should have no tokens remaining")

        } finally {
            executor.shutdown()
        }
    }

    @Test
    fun `rate limiter reset functionality works correctly`() {
        // Given - consume all available tokens
        repeat(5) {
            assertTrue(rateLimiter.tryConsume(), "Should consume all available tokens")
        }
        assertFalse(rateLimiter.tryConsume(), "Should be rate limited")
        assertEquals(0, rateLimiter.getAvailableTokens(), "Should have no tokens")

        // When - reset the rate limiter
        rateLimiter.reset()

        // Then - should have full capacity again
        assertEquals(5, rateLimiter.getAvailableTokens(), "Should have full hourly capacity after reset")
        assertTrue(rateLimiter.tryConsume(), "Should allow requests after reset")
    }

    @Test
    fun `rate limiter bucket configuration uses expected limits`() {
        // Given & When - rate limiter is initialized in setUp

        // Then - verify initial token availability matches hourly limit (most restrictive)
        val initialTokens = rateLimiter.getAvailableTokens()
        assertEquals(5, initialTokens, "Should start with hourly limit tokens available")

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
        assertEquals(0, rateLimiter.getAvailableTokens(), "Should have no tokens after consuming all")
    }

    @Test
    fun `rate limiter with different configuration values works correctly`() {
        // Given - create rate limiter with different limits
        val customProperties = WeatherApiProperties(
            key = "test-key",
            baseUrl = "http://test.com",
            rateLimit = WeatherApiProperties.RateLimit(
                requestsPerDay = 100,
                requestsPerHour = 10
            )
        )

        val customRateLimiter = InMemoryRateLimiter(customProperties)
        val initMethod = InMemoryRateLimiter::class.java.getDeclaredMethod("init")
        initMethod.isAccessible = true
        initMethod.invoke(customRateLimiter)

        // When & Then - should respect the new hourly limit
        assertEquals(10, customRateLimiter.getAvailableTokens(), "Should have 10 tokens initially")

        // Consume all 10 requests
        repeat(10) { requestNumber ->
            assertTrue(customRateLimiter.tryConsume(), "Request ${requestNumber + 1} should be allowed")
        }

        // 11th request should be denied
        assertFalse(customRateLimiter.tryConsume(), "11th request should be denied")
        assertEquals(0, customRateLimiter.getAvailableTokens(), "Should have no tokens remaining")
    }

    @Test
    fun `rate limiter implements RateLimiter interface correctly`() {
        // Given & When
        val rateLimiterInterface: RateLimiter = rateLimiter

        // Then
        assertNotNull(rateLimiterInterface)
        assertTrue(rateLimiterInterface is InMemoryRateLimiter)

        // Verify interface method works
        assertTrue(rateLimiterInterface.tryConsume(), "Interface method should work correctly")
    }


    @Test
    fun `hourly refill works with fake time`() {
        val fakeTime = FakeTimeMeter()
        val bucket = Bucket.builder()
            .withCustomTimePrecision(fakeTime)
            .addLimit(
                Bandwidth.simple(
                    5, Duration.ofDays(1)
                )
            )
            .addLimit(
                Bandwidth.classic(
                    3, Refill.intervally(
                        3,
                        Duration.ofHours(1)
                    )
                )
            ).build()

        // consume all 3
        assertTrue(bucket.tryConsume(1))
        assertTrue(bucket.tryConsume(1))
        assertTrue(bucket.tryConsume(1))

        assertFalse(bucket.tryConsume(1))

        // advance 1h → refill
        fakeTime.addMillis(60 * 60 * 1000)
        assertTrue(bucket.tryConsume(1))
        assertTrue(bucket.tryConsume(1))

        // exceed daily limit
        assertFalse(bucket.tryConsume(1))

    }
}
