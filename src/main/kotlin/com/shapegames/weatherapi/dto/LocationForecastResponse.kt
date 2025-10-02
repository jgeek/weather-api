package com.shapegames.weatherapi.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "5-day weather forecast for a specific location")
data class LocationForecastResponse(
    @Schema(description = "Unique identifier for the location", example = "2345")
    val locationId: String,

    @Schema(description = "Human-readable name of the location", example = "London")
    val locationName: String,

    @Schema(description = "Temperature unit used", example = "celsius", allowableValues = ["celsius", "fahrenheit"])
    val unit: String,

    @Schema(description = "List of daily forecasts for the next 5 days")
    val forecast: List<DayForecast>
)

@Schema(description = "Weather forecast for a single day")
data class DayForecast(
    @Schema(description = "Date in YYYY-MM-DD format", example = "2025-10-03")
    val date: String,

    @Schema(description = "Average temperature for the day", example = "22.5")
    val temperature: Double,

    @Schema(description = "Weather description", example = "Clear sky")
    val description: String,

    @Schema(description = "Humidity percentage", example = "65", minimum = "0", maximum = "100")
    val humidity: Int,

    @Schema(description = "Wind speed in m/s", example = "3.2")
    val windSpeed: Double
)
