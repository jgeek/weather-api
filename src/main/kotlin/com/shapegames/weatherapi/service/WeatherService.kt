package com.shapegames.weatherapi.service

import com.shapegames.weatherapi.dto.*
import com.shapegames.weatherapi.exception.WeatherApiException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@Service
class WeatherService(
    private val openWeatherMapClient: OpenWeatherMapClient
) {

    private val logger = LoggerFactory.getLogger(WeatherService::class.java)

    fun getWeatherSummary(
        locationIds: List<String>,
        temperatureThreshold: Int,
        unit: String
    ): WeatherSummaryResponse {
        logger.info("Getting weather summary for ${locationIds.size} locations with threshold $temperatureThreshold°$unit")

        val locationSummaries = locationIds.mapNotNull { locationId ->
            try {
                val forecast = openWeatherMapClient.getForecast(locationId)
                val tomorrowForecast = getTomorrowTemperature(forecast)

                if (tomorrowForecast != null) {
                    val temperature = convertTemperature(tomorrowForecast.main.temp, unit)
                    LocationSummary(
                        locationId = locationId,
                        locationName = forecast.city.name,
                        tomorrowTemperature = temperature,
                        willExceedThreshold = temperature > temperatureThreshold
                    )
                } else {
                    logger.warn("No tomorrow forecast available for location: $locationId")
                    null
                }
            } catch (ex: Exception) {
                logger.error("Failed to get weather for location: $locationId", ex)
                null
            }
        }

        return WeatherSummaryResponse(
            locations = locationSummaries.filter { it.willExceedThreshold },
            unit = unit,
            temperatureThreshold = temperatureThreshold
        )
    }

    fun getLocationForecast(locationId: String, unit: String = "celsius"): LocationForecastResponse {
        logger.info("Getting 5-day forecast for location: $locationId")

        val forecast = openWeatherMapClient.getForecast(locationId)
        val dailyForecasts = groupForecastByDay(forecast, unit)

        return LocationForecastResponse(
            locationId = locationId,
            locationName = forecast.city.name,
            unit = unit,
            forecast = dailyForecasts.take(5) // Limit to 5 days
        )
    }

    private fun getTomorrowTemperature(forecast: OpenWeatherForecastResponse): ForecastItem? {
        val tomorrow = LocalDate.now().plusDays(1)
        val tomorrowStr = tomorrow.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

        return forecast.list
            .filter { it.dtTxt.startsWith(tomorrowStr) }
            .minByOrNull {
                // Get the forecast closest to noon for more accurate daily temperature
                val time = it.dtTxt.substring(11, 16)
                kotlin.math.abs(time.replace(":", "").toInt() - 1200)
            }
    }

    private fun groupForecastByDay(forecast: OpenWeatherForecastResponse, unit: String): List<DayForecast> {
        return forecast.list
            .groupBy { it.dtTxt.substring(0, 10) } // Group by date (YYYY-MM-DD)
            .map { (date, forecasts) ->
                // Take the forecast closest to noon for each day
                val midDayForecast = forecasts.minBy {
                    val time = it.dtTxt.substring(11, 16)
                    kotlin.math.abs(time.replace(":", "").toInt() - 1200)
                }

                DayForecast(
                    date = date,
                    temperature = convertTemperature(midDayForecast.main.temp, unit),
                    description = midDayForecast.weather.firstOrNull()?.description ?: "Unknown",
                    humidity = midDayForecast.main.humidity,
                    windSpeed = midDayForecast.wind.speed
                )
            }
            .sortedBy { it.date }
    }

    private fun convertTemperature(celsius: Double, unit: String): Double {
        return when (unit.lowercase()) {
            "fahrenheit" -> (celsius * 9/5) + 32
            "celsius" -> celsius
            else -> throw WeatherApiException("Unsupported temperature unit: $unit. Use 'celsius' or 'fahrenheit'")
        }.let { (it * 10).roundToInt() / 10.0 } // Round to 1 decimal place
    }
}
