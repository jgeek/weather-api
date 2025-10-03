package com.shapegames.weatherapi.exception

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class WeatherExceptionsTest {

    @Test
    fun `WeatherApiException creates with message only`() {
        // Given
        val message = "Test weather API error"

        // When
        val exception = WeatherApiException(message)

        // Then
        assertEquals(message, exception.message)
        assertNull(exception.cause)
    }

    @Test
    fun `WeatherApiException creates with message and cause`() {
        // Given
        val message = "Test weather API error"
        val cause = IllegalArgumentException("Root cause")

        // When
        val exception = WeatherApiException(message, cause)

        // Then
        assertEquals(message, exception.message)
        assertEquals(cause, exception.cause)
    }

    @Test
    fun `WeatherApiException inherits from RuntimeException`() {
        // Given
        val exception = WeatherApiException("test")

        // Then
        assertTrue(exception::class.java.superclass == RuntimeException::class.java)
    }

    @Test
    fun `RateLimitExceededException creates with message`() {
        // Given
        val message = "Rate limit exceeded"

        // When
        val exception = RateLimitExceededException(message)

        // Then
        assertEquals(message, exception.message)
        assertNull(exception.cause)
    }

    @Test
    fun `RateLimitExceededException inherits from RuntimeException`() {
        // Given
        val exception = RateLimitExceededException("test")

        // Then
        assertTrue(exception::class.java.superclass == RuntimeException::class.java)
    }
}
