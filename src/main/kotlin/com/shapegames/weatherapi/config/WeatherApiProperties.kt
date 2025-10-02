package com.shapegames.weatherapi.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.bind.ConstructorBinding

@ConfigurationProperties(prefix = "weather.api")
data class WeatherApiProperties @ConstructorBinding constructor(
    val key: String,
    val baseUrl: String,
    val rateLimit: RateLimit
) {
    data class RateLimit(
        val requestsPerDay: Int,
        val windowMinutes: Int
    )
}

@ConfigurationProperties(prefix = "weather.cache")
data class CacheProperties @ConstructorBinding constructor(
    val ttl: Long
)

@ConfigurationProperties(prefix = "weather.client")
data class ClientProperties @ConstructorBinding constructor(
    val timeout: Long
)
