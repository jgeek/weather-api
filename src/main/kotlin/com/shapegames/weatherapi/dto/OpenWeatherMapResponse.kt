package com.shapegames.weatherapi.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.io.Serializable

// OpenWeatherMap API response models
data class OpenWeatherForecastResponse(
    @JsonProperty("cod") val cod: String,
    @JsonProperty("message") val message: Int,
    @JsonProperty("cnt") val cnt: Int,
    @JsonProperty("list") val list: List<ForecastItem>,
    @JsonProperty("city") val city: City
) : Serializable

data class ForecastItem(
    @JsonProperty("dt") val dt: Long,
    @JsonProperty("dt_txt") val dtTxt: String?,
    @JsonProperty("main") val main: MainWeather,
    @JsonProperty("weather") val weather: List<Weather>,
    @JsonProperty("clouds") val clouds: Clouds,
    @JsonProperty("wind") val wind: Wind,
    @JsonProperty("visibility") val visibility: Int?,
    @JsonProperty("pop") val pop: Double,
    @JsonProperty("sys") val sys: Sys
) : Serializable

data class MainWeather(
    @JsonProperty("temp") val temp: Double,
    @JsonProperty("feels_like") val feelsLike: Double,
    @JsonProperty("temp_min") val tempMin: Double,
    @JsonProperty("temp_max") val tempMax: Double,
    @JsonProperty("pressure") val pressure: Int,
    @JsonProperty("sea_level") val seaLevel: Int?,
    @JsonProperty("grnd_level") val grndLevel: Int?,
    @JsonProperty("humidity") val humidity: Int,
    @JsonProperty("temp_kf") val tempKf: Double?
) : Serializable

data class Weather(
    @JsonProperty("id") val id: Int,
    @JsonProperty("main") val main: String,
    @JsonProperty("description") val description: String,
    @JsonProperty("icon") val icon: String
) : Serializable

data class Clouds(
    @JsonProperty("all") val all: Int
) : Serializable

data class Wind(
    @JsonProperty("speed") val speed: Double,
    @JsonProperty("deg") val deg: Int,
    @JsonProperty("gust") val gust: Double?
) : Serializable

data class Sys(
    @JsonProperty("pod") val pod: String
) : Serializable

data class City(
    @JsonProperty("id") val id: Long,
    @JsonProperty("name") val name: String,
    @JsonProperty("coord") val coord: Coord,
    @JsonProperty("country") val country: String,
    @JsonProperty("population") val population: Long,
    @JsonProperty("timezone") val timezone: Int,
    @JsonProperty("sunrise") val sunrise: Long,
    @JsonProperty("sunset") val sunset: Long
) : Serializable

data class Coord(
    @JsonProperty("lat") val lat: Double,
    @JsonProperty("lon") val lon: Double
) : Serializable
