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
import jakarta.validation.constraints.*
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/weather")
@Tag(name = "Weather API", description = "Rate-limited weather data integration layer")
@Validated
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
        @RequestParam(defaultValue = "celsius")
        @NotBlank(message = "Unit parameter cannot be blank")
        @Pattern(regexp = "^(celsius|fahrenheit)$", message = "Unit must be either 'celsius' or 'fahrenheit'")
        unit: String,

        @Parameter(
            description = "Minimum temperature threshold",
            example = "24",
            required = true
        )
        @RequestParam
        @Min(value = -100, message = "Temperature must be at least -100")
        @Max(value = 100, message = "Temperature must be at most 100")
        temperature: Int,

        @Parameter(
            description = "Comma-separated list of location IDs",
            example = "2988507,2643743,2950159,2618425",
            required = true
        )
        @RequestParam
        @NotBlank(message = "Locations parameter cannot be blank")
        @Pattern(regexp = "^\\d+(,\\s*\\d+)*$", message = "Locations must be comma-separated numeric IDs")
        locations: String
    ): ResponseEntity<WeatherSummaryResponse> {
        logger.info("Received request for weather summary: unit=$unit, temperature=$temperature, locations=$locations")

        val locationIds = locations.split(",").map { it.trim() }.filter { it.isNotBlank() }
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
            example = "2618425",
            required = true
        )
        @PathVariable
        @NotBlank(message = "Location ID cannot be blank")
        @Pattern(regexp = "^\\d+$", message = "Location ID must be numeric")
        locationId: String,

        @Parameter(
            description = "Temperature unit (celsius or fahrenheit)",
            example = "celsius",
            required = false
        )
        @RequestParam(defaultValue = "celsius")
        @NotBlank(message = "Unit parameter cannot be blank")
        @Pattern(regexp = "^(celsius|fahrenheit)$", message = "Unit must be either 'celsius' or 'fahrenheit'")
        unit: String = "celsius"
    ): ResponseEntity<LocationForecastResponse> {
        logger.info("Received request for location forecast: locationId=$locationId, unit=$unit")

        val response = weatherService.getLocationForecast(locationId, unit)
        return ResponseEntity.ok(response)
    }
}
