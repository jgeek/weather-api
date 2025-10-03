package com.shapegames.weatherapi.service

import org.springframework.stereotype.Service

@Service
class ThirdPartyService(
    private val rateLimiter: RateLimiter
) {
    fun callApi(): String {
        return if (rateLimiter.tryConsume()) {
            // TODO: actual third-party API call
            "API response"
        } else {
            throw RuntimeException("Rate limit exceeded for third-party API")
        }
    }
}
