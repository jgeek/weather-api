package com.shapegames.weatherapi.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class WeatherApiConfig {

    @Bean
    fun webClient(): WebClient {
        return WebClient.builder().build()
    }
}
