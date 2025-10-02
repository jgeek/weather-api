package com.shapegames.weatherapi.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.io.Serializable

// OpenWeatherMap API response models
data class OpenWeatherForecastResponse(
    val cod: String,
    val message: Int,
    val cnt: Int,
    val list: List<ForecastItem>,
    val city: City
) : Serializable

data class ForecastItem(
    val dt: Long,
    @JsonProperty("dt_txt")
    val dtTxt: String,
    val main: MainWeather,
    val weather: List<Weather>,
    val clouds: Clouds,
    val wind: Wind,
    val visibility: Int,
    val pop: Double,
    val sys: Sys
) : Serializable

data class MainWeather(
    val temp: Double,
    @JsonProperty("feels_like")
    val feelsLike: Double,
    @JsonProperty("temp_min")
    val tempMin: Double,
    @JsonProperty("temp_max")
    val tempMax: Double,
    val pressure: Int,
    @JsonProperty("sea_level")
    val seaLevel: Int,
    @JsonProperty("grnd_level")
    val grndLevel: Int,
    val humidity: Int,
    @JsonProperty("temp_kf")
    val tempKf: Double
) : Serializable

data class Weather(
    val id: Int,
    val main: String,
    val description: String,
    val icon: String
) : Serializable

data class Clouds(
    val all: Int
) : Serializable

data class Wind(
    val speed: Double,
    val deg: Int,
    val gust: Double?
) : Serializable

data class Sys(
    val pod: String
) : Serializable

data class City(
    val id: Long,
    val name: String,
    val coord: Coord,
    val country: String,
    val population: Long,
    val timezone: Int,
    val sunrise: Long,
    val sunset: Long
) : Serializable

data class Coord(
    val lat: Double,
    val lon: Double
) : Serializable
