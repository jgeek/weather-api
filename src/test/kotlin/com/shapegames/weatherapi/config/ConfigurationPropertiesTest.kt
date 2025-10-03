package com.shapegames.weatherapi.config

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class WeatherApiPropertiesTest {

    @Test
    fun `WeatherApiProperties creates instance with all properties`() {
        // Given
        val key = "test-api-key"
        val baseUrl = "https://api.openweathermap.org/data/2.5"
        val rateLimit = WeatherApiProperties.RateLimit(1000, 100)

        // When
        val properties = WeatherApiProperties(key, baseUrl, rateLimit)

        // Then
        assertEquals(key, properties.key)
        assertEquals(baseUrl, properties.baseUrl)
        assertEquals(rateLimit, properties.rateLimit)
    }

    @Test
    fun `RateLimit creates instance with correct values`() {
        // Given
        val requestsPerDay = 1000
        val requestsPerHour = 100
        val hourlyWindowMinutes = 60
        val dailyWindowMinutes = 1440

        // When
        val rateLimit = WeatherApiProperties.RateLimit(requestsPerDay, requestsPerHour)

        // Then
        assertEquals(requestsPerDay, rateLimit.requestsPerDay)
        assertEquals(requestsPerHour, rateLimit.requestsPerHour)
    }

    @Test
    fun `WeatherApiProperties data class equality works correctly`() {
        // Given
        val rateLimit1 = WeatherApiProperties.RateLimit(1000, 100)
        val rateLimit2 = WeatherApiProperties.RateLimit(1000, 100)
        val properties1 = WeatherApiProperties("key", "url", rateLimit1)
        val properties2 = WeatherApiProperties("key", "url", rateLimit2)

        // Then
        assertEquals(properties1, properties2)
        assertEquals(properties1.hashCode(), properties2.hashCode())
        assertNotSame(properties1, properties2)
    }
}

class CachePropertiesTest {

    @Test
    fun `CacheProperties creates instance with TTL`() {
        // Given
        val ttl = 3600L

        // When
        val properties = CacheProperties(ttl)

        // Then
        assertEquals(ttl, properties.ttl)
    }

    @Test
    fun `CacheProperties data class equality works correctly`() {
        // Given
        val properties1 = CacheProperties(3600L)
        val properties2 = CacheProperties(3600L)

        // Then
        assertEquals(properties1, properties2)
        assertEquals(properties1.hashCode(), properties2.hashCode())
    }
}

class ClientPropertiesTest {

    @Test
    fun `ClientProperties creates instance with timeout`() {
        // Given
        val timeout = 5000L

        // When
        val properties = ClientProperties(timeout)

        // Then
        assertEquals(timeout, properties.timeout)
    }

    @Test
    fun `ClientProperties data class equality works correctly`() {
        // Given
        val properties1 = ClientProperties(5000L)
        val properties2 = ClientProperties(5000L)

        // Then
        assertEquals(properties1, properties2)
        assertEquals(properties1.hashCode(), properties2.hashCode())
    }
}
