package com.shapegames.weatherapi.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "Error response structure for API exceptions")
data class ErrorResponse(
    @Schema(description = "HTTP status code", example = "400")
    val status: Int,

    @Schema(description = "Error message describing what went wrong", example = "Invalid temperature unit provided")
    val message: String,

    @Schema(description = "Timestamp when the error occurred", example = "2025-10-02T14:30:00")
    val timestamp: LocalDateTime = LocalDateTime.now(),

    @Schema(description = "Request path where the error occurred", example = "/weather/summary")
    val path: String? = null
)
