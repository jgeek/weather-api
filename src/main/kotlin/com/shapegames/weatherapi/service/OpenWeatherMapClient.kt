package com.shapegames.weatherapi.service

import com.shapegames.weatherapi.config.WeatherApiProperties
import com.shapegames.weatherapi.dto.OpenWeatherForecastResponse
import com.shapegames.weatherapi.exception.RateLimitExceededException
import com.shapegames.weatherapi.exception.WeatherApiException
import lombok.extern.slf4j.Slf4j
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import java.time.Duration

@Slf4j
@Service
class OpenWeatherMapClient(
    private val webClient: WebClient,
    private val weatherApiProperties: WeatherApiProperties,
    private val rateLimitService: RateLimitService
) {

    private val logger = LoggerFactory.getLogger(OpenWeatherMapClient::class.java)

    @Cacheable("weather-forecast", key = "#locationId")
    fun getForecast(locationId: String): OpenWeatherForecastResponse {
        if (!rateLimitService.canMakeRequest()) {
            throw RateLimitExceededException("Daily rate limit exceeded for OpenWeatherMap API")
        }

        try {
            logger.info("Making API request for location: $locationId")
            val response = webClient.get()
                .uri("${weatherApiProperties.baseUrl}/forecast") { uriBuilder ->
                    uriBuilder
                        .queryParam("id", locationId)
                        .queryParam("appid", weatherApiProperties.key)
                        .queryParam("units", "metric")
                        .build()
                }
                .retrieve()
                // print response body
                .bodyToMono(OpenWeatherForecastResponse::class.java)
                .timeout(Duration.ofMillis(5000))
                .doOnNext {
                    rateLimitService.incrementRequestCount()
                    logger.info("Successfully retrieved forecast for location: $locationId")
                }
                .doOnError { error ->
                    logger.error("Error retrieving forecast for location: $locationId", error)
                }
                .block()

            return response ?: throw WeatherApiException("Empty response from OpenWeatherMap API")

        } catch (ex: WebClientResponseException) {
            when (ex.statusCode.value()) {
                404 -> throw WeatherApiException("Location not found: $locationId")
                401 -> throw WeatherApiException("Invalid API key")
                429 -> throw RateLimitExceededException("Rate limit exceeded by OpenWeatherMap")
                else -> throw WeatherApiException("API error: ${ex.message}")
            }
        } catch (ex: Exception) {
            logger.error("Unexpected error calling OpenWeatherMap API", ex)
            throw WeatherApiException("Failed to retrieve weather data: ${ex.message}")
        }
    }
}
