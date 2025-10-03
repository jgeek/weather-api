package com.shapegames.weatherapi.config

import io.lettuce.core.RedisClient
import org.springframework.boot.autoconfigure.data.redis.RedisProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RedisConfig(
    private val redisProperties: RedisProperties
) {

    @Bean
    fun redisClient(): RedisClient {
        val connectionString = buildRedisConnectionString()
        return RedisClient.create(connectionString)
    }

    private fun buildRedisConnectionString(): String {
        val host = redisProperties.host ?: "localhost"
        val port = redisProperties.port
        val password = redisProperties.password

        return if (!password.isNullOrEmpty()) {
            "redis://:$password@$host:$port"
        } else {
            "redis://$host:$port"
        }
    }
}
