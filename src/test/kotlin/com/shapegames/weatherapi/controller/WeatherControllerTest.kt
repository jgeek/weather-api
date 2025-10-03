package com.shapegames.weatherapi.controller

import com.shapegames.weatherapi.dto.*
import com.shapegames.weatherapi.service.WeatherService
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@WebMvcTest(WeatherController::class)
class WeatherControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var weatherService: WeatherService

    @Test
    fun `getWeatherSummary returns successful response with valid parameters`() {
        val expectedResponse = WeatherSummaryResponse(
            locations = listOf(
                LocationSummary(
                    locationId = "2988507",
                    locationName = "Berlin",
                    tomorrowTemperature = 25.0,
                    willExceedThreshold = true
                )
            ),
            unit = "celsius",
            temperatureThreshold = 24
        )

        `when`(weatherService.getWeatherSummary(listOf("2988507"), 24, "celsius"))
            .thenReturn(expectedResponse)

        mockMvc.perform(
            get("/weather/summary")
                .param("temperature", "24")
                .param("locations", "2988507")
                .param("unit", "celsius")
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.unit").value("celsius"))
            .andExpect(jsonPath("$.temperatureThreshold").value(24))
            .andExpect(jsonPath("$.locations[0].locationId").value("2988507"))
            .andExpect(jsonPath("$.locations[0].locationName").value("Berlin"))
    }

    @Test
    fun `getWeatherSummary uses default celsius unit when unit parameter is not provided`() {
        val expectedResponse = WeatherSummaryResponse(
            locations = emptyList(),
            unit = "celsius",
            temperatureThreshold = 20
        )

        `when`(weatherService.getWeatherSummary(listOf("12345"), 20, "celsius"))
            .thenReturn(expectedResponse)

        mockMvc.perform(
            get("/weather/summary")
                .param("temperature", "20")
                .param("locations", "12345")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.unit").value("celsius"))
    }

    @Test
    fun `getWeatherSummary handles fahrenheit unit parameter correctly`() {
        val expectedResponse = WeatherSummaryResponse(
            locations = emptyList(),
            unit = "fahrenheit",
            temperatureThreshold = 75
        )

        `when`(weatherService.getWeatherSummary(listOf("54321"), 75, "fahrenheit"))
            .thenReturn(expectedResponse)

        mockMvc.perform(
            get("/weather/summary")
                .param("temperature", "75")
                .param("locations", "54321")
                .param("unit", "fahrenheit")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.unit").value("fahrenheit"))
    }

    @Test
    fun `getWeatherSummary handles multiple comma-separated location IDs`() {
        val expectedResponse = WeatherSummaryResponse(
            locations = listOf(
                LocationSummary("2988507", "Berlin", 25.0, true),
                LocationSummary("2643743", "London", 22.0, false)
            ),
            unit = "celsius",
            temperatureThreshold = 24
        )

        `when`(weatherService.getWeatherSummary(listOf("2988507", "2643743"), 24, "celsius"))
            .thenReturn(expectedResponse)

        mockMvc.perform(
            get("/weather/summary")
                .param("temperature", "24")
                .param("locations", "2988507,2643743")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.locations").isArray)
            .andExpect(jsonPath("$.locations.length()").value(2))
    }

    @Test
    fun `getWeatherSummary handles location IDs with spaces around commas`() {
        val expectedResponse = WeatherSummaryResponse(
            locations = emptyList(),
            unit = "celsius",
            temperatureThreshold = 24
        )

        `when`(weatherService.getWeatherSummary(listOf("2988507", "2643743"), 24, "celsius"))
            .thenReturn(expectedResponse)

        mockMvc.perform(
            get("/weather/summary")
                .param("temperature", "24")
                .param("locations", "2988507, 2643743")
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `getWeatherSummary returns bad request for invalid unit parameter`() {
        mockMvc.perform(
            get("/weather/summary")
                .param("temperature", "24")
                .param("locations", "12345")
                .param("unit", "kelvin")
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `getWeatherSummary returns bad request for missing temperature parameter`() {
        mockMvc.perform(
            get("/weather/summary")
                .param("locations", "12345")
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `getWeatherSummary returns bad request for missing locations parameter`() {
        mockMvc.perform(
            get("/weather/summary")
                .param("temperature", "24")
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `getWeatherSummary returns bad request for temperature below minimum`() {
        mockMvc.perform(
            get("/weather/summary")
                .param("temperature", "-101")
                .param("locations", "12345")
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `getWeatherSummary returns bad request for temperature above maximum`() {
        mockMvc.perform(
            get("/weather/summary")
                .param("temperature", "101")
                .param("locations", "12345")
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `getWeatherSummary returns bad request for blank locations parameter`() {
        mockMvc.perform(
            get("/weather/summary")
                .param("temperature", "24")
                .param("locations", "   ")
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `getWeatherSummary returns bad request for non-numeric location IDs`() {
        mockMvc.perform(
            get("/weather/summary")
                .param("temperature", "24")
                .param("locations", "abc123,def456")
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `getWeatherSummary returns bad request for mixed valid and invalid location IDs`() {
        mockMvc.perform(
            get("/weather/summary")
                .param("temperature", "24")
                .param("locations", "12345,abc123")
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `getLocationForecast returns successful response with valid location ID`() {
        val expectedResponse = LocationForecastResponse(
            locationId = "2618425",
            locationName = "Copenhagen",
            unit = "celsius",
            forecast = listOf(
                DayForecast("2025-10-03", 22.5, "Clear sky", 65, 3.2),
                DayForecast("2025-10-04", 20.1, "Partly cloudy", 70, 2.8)
            )
        )

        `when`(weatherService.getLocationForecast("2618425", "celsius"))
            .thenReturn(expectedResponse)

        mockMvc.perform(
            get("/weather/locations/2618425")
                .param("unit", "celsius")
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.locationId").value("2618425"))
            .andExpect(jsonPath("$.locationName").value("Copenhagen"))
            .andExpect(jsonPath("$.unit").value("celsius"))
            .andExpect(jsonPath("$.forecast").isArray)
            .andExpect(jsonPath("$.forecast.length()").value(2))
    }

    @Test
    fun `getLocationForecast uses default celsius unit when unit parameter is not provided`() {
        val expectedResponse = LocationForecastResponse(
            locationId = "2618425",
            locationName = "Copenhagen",
            unit = "celsius",
            forecast = emptyList()
        )

        `when`(weatherService.getLocationForecast("2618425", "celsius"))
            .thenReturn(expectedResponse)

        mockMvc.perform(get("/weather/locations/2618425"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.unit").value("celsius"))
    }

    @Test
    fun `getLocationForecast handles fahrenheit unit parameter correctly`() {
        val expectedResponse = LocationForecastResponse(
            locationId = "2618425",
            locationName = "Copenhagen",
            unit = "fahrenheit",
            forecast = emptyList()
        )

        `when`(weatherService.getLocationForecast("2618425", "fahrenheit"))
            .thenReturn(expectedResponse)

        mockMvc.perform(
            get("/weather/locations/2618425")
                .param("unit", "fahrenheit")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.unit").value("fahrenheit"))
    }

    @Test
    fun `getLocationForecast returns bad request for non-numeric location ID`() {
        mockMvc.perform(get("/weather/locations/abc123"))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `getLocationForecast returns bad request for location ID with special characters`() {
        mockMvc.perform(get("/weather/locations/123-456"))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `getLocationForecast returns bad request for location ID with spaces`() {
        mockMvc.perform(get("/weather/locations/123 456"))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `getLocationForecast returns not found for empty location ID`() {
        mockMvc.perform(get("/weather/locations/"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `getLocationForecast returns bad request for invalid unit parameter`() {
        mockMvc.perform(
            get("/weather/locations/2618425")
                .param("unit", "kelvin")
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `getLocationForecast uses default value 'celsius' instead of blank unit parameter`() {
        mockMvc.perform(
            get("/weather/locations/2618425")
                .param("unit", "")
        )
            .andExpect(status().isOk)
    }
}
