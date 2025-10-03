package com.shapegames.weatherapi.service

import com.shapegames.weatherapi.dto.*
import com.shapegames.weatherapi.exception.WeatherApiException
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.mockito.Mockito.`when`
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class WeatherServiceTest {

    private val openWeatherMapClient = mock(OpenWeatherMapClient::class.java)
    private val weatherService = WeatherService(openWeatherMapClient)

    @Test
    fun `getWeatherSummary returns locations exceeding threshold`() {
        // Given
        val locationIds = listOf("123", "456")
        val threshold = 20
        val unit = "celsius"
        val tomorrow = LocalDate.now().plusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

        val forecast1 = createMockForecast("123", "Berlin", tomorrow, 25.0)
        val forecast2 = createMockForecast("456", "Paris", tomorrow, 15.0)

        `when`(openWeatherMapClient.getForecast("123")).thenReturn(forecast1)
        `when`(openWeatherMapClient.getForecast("456")).thenReturn(forecast2)

        // When
        val result = weatherService.getWeatherSummary(locationIds, threshold, unit)

        // Then
        assertEquals(1, result.locations.size)
        assertEquals("123", result.locations[0].locationId)
        assertEquals("Berlin", result.locations[0].locationName)
        assertEquals(25.0, result.locations[0].tomorrowTemperature)
        assertTrue(result.locations[0].willExceedThreshold)
        assertEquals(unit, result.unit)
        assertEquals(threshold, result.temperatureThreshold)
    }

    @Test
    fun `getWeatherSummary handles fahrenheit conversion`() {
        // Given
        val locationIds = listOf("123")
        val threshold = 70
        val unit = "fahrenheit"
        val tomorrow = LocalDate.now().plusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

        val forecast = createMockForecast("123", "Berlin", tomorrow, 25.0) // 25°C = 77°F

        `when`(openWeatherMapClient.getForecast("123")).thenReturn(forecast)

        // When
        val result = weatherService.getWeatherSummary(locationIds, threshold, unit)

        // Then
        assertEquals(1, result.locations.size)
        assertEquals(77.0, result.locations[0].tomorrowTemperature)
        assertTrue(result.locations[0].willExceedThreshold)
    }

    @Test
    fun `getWeatherSummary filters out locations not exceeding threshold`() {
        // Given
        val locationIds = listOf("123", "456")
        val threshold = 30
        val unit = "celsius"
        val tomorrow = LocalDate.now().plusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

        val forecast1 = createMockForecast("123", "Berlin", tomorrow, 25.0)
        val forecast2 = createMockForecast("456", "Paris", tomorrow, 20.0)

        `when`(openWeatherMapClient.getForecast("123")).thenReturn(forecast1)
        `when`(openWeatherMapClient.getForecast("456")).thenReturn(forecast2)

        // When
        val result = weatherService.getWeatherSummary(locationIds, threshold, unit)

        // Then
        assertEquals(0, result.locations.size)
    }

    @Test
    fun `getWeatherSummary handles API exceptions gracefully`() {
        // Given
        val locationIds = listOf("123", "456")
        val threshold = 20
        val unit = "celsius"
        val tomorrow = LocalDate.now().plusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

        val forecast = createMockForecast("456", "Paris", tomorrow, 25.0)

        `when`(openWeatherMapClient.getForecast("123")).thenThrow(WeatherApiException("Location not found"))
        `when`(openWeatherMapClient.getForecast("456")).thenReturn(forecast)

        // When
        val result = weatherService.getWeatherSummary(locationIds, threshold, unit)

        // Then
        assertEquals(1, result.locations.size)
        assertEquals("456", result.locations[0].locationId)
    }

    @Test
    fun `getLocationForecast returns 5-day forecast`() {
        // Given
        val locationId = "123"
        val unit = "celsius"
        val forecast = createMockMultiDayForecast("123", "Berlin")

        `when`(openWeatherMapClient.getForecast(locationId)).thenReturn(forecast)

        // When
        val result = weatherService.getLocationForecast(locationId, unit)

        // Then
        assertEquals(locationId, result.locationId)
        assertEquals("Berlin", result.locationName)
        assertEquals(unit, result.unit)
        assertEquals(5, result.forecast.size)
        assertTrue(result.forecast.all { it.temperature > 0 })
    }

    private fun createMockForecast(
        locationId: String,
        cityName: String,
        tomorrowDate: String,
        temperature: Double
    ): OpenWeatherForecastResponse {
        return OpenWeatherForecastResponse(
            cod = "200",
            message = 0,
            cnt = 1,
            list = listOf(
                ForecastItem(
                    dt = System.currentTimeMillis() / 1000,
                    dtTxt = "$tomorrowDate 12:00:00",
                    main = MainWeather(
                        temp = temperature,
                        feelsLike = temperature + 2,
                        tempMin = temperature - 2,
                        tempMax = temperature + 2,
                        pressure = 1013,
                        seaLevel = 1013,
                        grndLevel = 1010,
                        humidity = 60,
                        tempKf = 0.0
                    ),
                    weather = listOf(Weather(id = 800, main = "Clear", description = "clear sky", icon = "01d")),
                    clouds = Clouds(all = 0),
                    wind = Wind(speed = 5.0, deg = 180, gust = null),
                    visibility = 10000,
                    pop = 0.0,
                    sys = Sys(pod = "d")
                )
            ),
            city = City(
                id = locationId.toLong(),
                name = cityName,
                coord = Coord(lat = 52.52, lon = 13.41),
                country = "DE",
                population = 1000000,
                timezone = 3600,
                sunrise = System.currentTimeMillis() / 1000,
                sunset = System.currentTimeMillis() / 1000 + 12 * 3600
            )
        )
    }

    private fun createMockMultiDayForecast(locationId: String, cityName: String): OpenWeatherForecastResponse {
        val today = LocalDate.now()
        val forecastItems = mutableListOf<ForecastItem>()

        // Create forecast items for next 7 days (service will limit to 5)
        for (i in 1..7) {
            val date = today.plusDays(i.toLong()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            forecastItems.add(
                ForecastItem(
                    dt = System.currentTimeMillis() / 1000 + i * 24 * 3600,
                    dtTxt = "$date 12:00:00",
                    main = MainWeather(
                        temp = 20.0 + i,
                        feelsLike = 22.0 + i,
                        tempMin = 18.0 + i,
                        tempMax = 22.0 + i,
                        pressure = 1013,
                        seaLevel = 1013,
                        grndLevel = 1010,
                        humidity = 60 + i,
                        tempKf = 0.0
                    ),
                    weather = listOf(Weather(id = 800, main = "Clear", description = "clear sky", icon = "01d")),
                    clouds = Clouds(all = 0),
                    wind = Wind(speed = 5.0 + i, deg = 180, gust = null),
                    visibility = 10000,
                    pop = 0.0,
                    sys = Sys(pod = "d")
                )
            )
        }

        return OpenWeatherForecastResponse(
            cod = "200",
            message = 0,
            cnt = forecastItems.size,
            list = forecastItems,
            city = City(
                id = locationId.toLong(),
                name = cityName,
                coord = Coord(lat = 52.52, lon = 13.41),
                country = "DE",
                population = 1000000,
                timezone = 3600,
                sunrise = System.currentTimeMillis() / 1000,
                sunset = System.currentTimeMillis() / 1000 + 12 * 3600
            )
        )
    }
}
