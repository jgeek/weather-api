package com.shapegames.weatherapi.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Weather summary response containing locations that meet temperature criteria")
data class WeatherSummaryResponse(
    @Schema(description = "List of locations with weather information", example = "[]")
    val locations: List<LocationSummary>,

    @Schema(description = "Temperature unit used", example = "celsius", allowableValues = ["celsius", "fahrenheit"])
    val unit: String,

    @Schema(description = "Temperature threshold used for filtering", example = "24")
    val temperatureThreshold: Int
)

@Schema(description = "Summary information for a specific location")
data class LocationSummary(
    @Schema(description = "Unique identifier for the location", example = "2345")
    val locationId: String,

    @Schema(description = "Human-readable name of the location", example = "London")
    val locationName: String,

    @Schema(description = "Tomorrow's temperature in the specified unit", example = "25.5")
    val tomorrowTemperature: Double,

    @Schema(description = "Whether tomorrow's temperature will exceed the threshold", example = "true")
    val willExceedThreshold: Boolean
)
