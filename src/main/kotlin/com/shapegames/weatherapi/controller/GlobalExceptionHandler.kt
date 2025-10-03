package com.shapegames.weatherapi.controller

import com.shapegames.weatherapi.exception.RateLimitExceededException
import com.shapegames.weatherapi.exception.WeatherApiException
import jakarta.validation.ConstraintViolationException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

data class ErrorResponse(
    val error: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

@RestControllerAdvice
class GlobalExceptionHandler {

    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(WeatherApiException::class)
    fun handleWeatherApiException(ex: WeatherApiException): ResponseEntity<ErrorResponse> {
        logger.error("Weather API error: ${ex.message}", ex)
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse("WEATHER_API_ERROR", ex.message ?: "Unknown weather API error"))
    }

    @ExceptionHandler(RateLimitExceededException::class)
    fun handleRateLimitExceededException(ex: RateLimitExceededException): ResponseEntity<ErrorResponse> {
        logger.warn("Rate limit exceeded: ${ex.message}")
        return ResponseEntity
            .status(HttpStatus.TOO_MANY_REQUESTS)
            .body(ErrorResponse("RATE_LIMIT_EXCEEDED", ex.message ?: "Rate limit exceeded"))
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(ex: IllegalArgumentException): ResponseEntity<ErrorResponse> {
        logger.warn("Invalid argument: ${ex.message}")
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse("INVALID_ARGUMENT", ex.message ?: "Invalid argument"))
    }

    @ExceptionHandler(Exception::class)
    fun handleGenericException(ex: Exception): ResponseEntity<ErrorResponse> {
        logger.error("Unexpected error", ex)
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ErrorResponse("INTERNAL_ERROR", "An unexpected error occurred"))
    }

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolationException(ex: ConstraintViolationException): ResponseEntity<ErrorResponse> {
        logger.warn("Validation constraint violation: ${ex.message}")
        val message = ex.constraintViolations.joinToString("; ") { "${it.propertyPath}: ${it.message}" }
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse("VALIDATION_ERROR", message))
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValidException(ex: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        logger.warn("Method argument validation error: ${ex.message}")
        val message = ex.bindingResult.fieldErrors.joinToString("; ") { "${it.field}: ${it.defaultMessage}" }
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse("VALIDATION_ERROR", message))
    }
}
