package com.shapegames.weatherapi.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
@EnableConfigurationProperties(WeatherApiProperties::class, CacheProperties::class, ClientProperties::class)
class WeatherApiConfig {

    @Bean
    fun webClient(): WebClient {
        return WebClient.builder().build()
    }
}
