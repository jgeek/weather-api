package com.shapegames.weatherapi.service

interface RateLimiter {
    fun tryConsume(): Boolean
}
