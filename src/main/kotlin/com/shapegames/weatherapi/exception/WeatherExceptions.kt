package com.shapegames.weatherapi.exception

class WeatherApiException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

class RateLimitExceededException(message: String) : RuntimeException(message)
