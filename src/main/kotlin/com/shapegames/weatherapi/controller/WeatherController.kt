package com.shapegames.weatherapi.controller

import com.shapegames.weatherapi.dto.WeatherSummaryResponse
import com.shapegames.weatherapi.dto.LocationForecastResponse
import com.shapegames.weatherapi.service.WeatherService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/weather")
@Tag(name = "Weather API", description = "Rate-limited weather data integration layer")
class WeatherController(private val weatherService: WeatherService) {

    private val logger = LoggerFactory.getLogger(WeatherController::class.java)

    @GetMapping("/summary")
    @Operation(
        summary = "Get weather summary for favorite locations",
        description = "Returns a list of user's favorite locations where the temperature will be above a certain threshold the next day"
    )
    @ApiResponses(value = [
        ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved weather summary",
            content = [Content(
                mediaType = "application/json",
                schema = Schema(implementation = WeatherSummaryResponse::class)
            )]
        ),
        ApiResponse(
            responseCode = "400",
            description = "Invalid request parameters",
            content = [Content()]
        ),
        ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content = [Content()]
        )
    ])
    fun getWeatherSummary(
        @Parameter(
            description = "Temperature unit (celsius or fahrenheit)",
            example = "celsius",
            required = false
        )
        @RequestParam(defaultValue = "celsius") unit: String,

        @Parameter(
            description = "Minimum temperature threshold",
            example = "24",
            required = true
        )
        @RequestParam temperature: Int,

        @Parameter(
            description = "Comma-separated list of location IDs",
            example = "2345,1456,7653",
            required = true
        )
        @RequestParam locations: String
    ): ResponseEntity<WeatherSummaryResponse> {
        logger.info("Received request for weather summary: unit=$unit, temperature=$temperature, locations=$locations")

        if (unit !in listOf("celsius", "fahrenheit")) {
            logger.warn("Invalid temperature unit: $unit")
            return ResponseEntity.badRequest().build()
        }

        if (locations.isBlank()) {
            logger.warn("Empty locations provided")
            return ResponseEntity.badRequest().build()
        }

        val locationIds = try {
            locations.split(",").map { it.trim() }.filter { it.isNotBlank() }
        } catch (e: Exception) {
            logger.warn("Invalid location IDs format: $locations")
            return ResponseEntity.badRequest().build()
        }

        if (locationIds.isEmpty()) {
            logger.warn("No valid location IDs provided")
            return ResponseEntity.badRequest().build()
        }

        val response = weatherService.getWeatherSummary(locationIds, temperature, unit)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/locations/{locationId}")
    @Operation(
        summary = "Get 5-day weather forecast for a location",
        description = "Returns temperature forecast for the next 5 days in a specific location"
    )
    @ApiResponses(value = [
        ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved location forecast",
            content = [Content(
                mediaType = "application/json",
                schema = Schema(implementation = LocationForecastResponse::class)
            )]
        ),
        ApiResponse(
            responseCode = "400",
            description = "Invalid location ID or parameters",
            content = [Content()]
        ),
        ApiResponse(
            responseCode = "404",
            description = "Location not found",
            content = [Content()]
        ),
        ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content = [Content()]
        )
    ])
    fun getLocationForecast(
        @Parameter(
            description = "Location ID to get forecast for",
            example = "2345",
            required = true
        )
        @PathVariable locationId: String,

        @Parameter(
            description = "Temperature unit (celsius or fahrenheit)",
            example = "celsius",
            required = false
        )
        @RequestParam(defaultValue = "celsius") unit: String = "celsius"
    ): ResponseEntity<LocationForecastResponse> {
        logger.info("Received request for location forecast: locationId=$locationId, unit=$unit")

        if (locationId.isBlank()) {
            logger.warn("Empty location ID provided")
            return ResponseEntity.badRequest().build()
        }

        if (unit !in listOf("celsius", "fahrenheit")) {
            logger.warn("Invalid temperature unit: $unit")
            return ResponseEntity.badRequest().build()
        }

        val response = weatherService.getLocationForecast(locationId, unit)
        return ResponseEntity.ok(response)
    }
}
