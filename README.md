# Weather API Service

A Spring Boot REST API service that acts as an integration layer between mobile apps and OpenWeatherMap API, with rate limiting and caching capabilities.

## Features

- **Rate Limiting**: Ensures compliance with OpenWeatherMap's 10,000 requests/day limit
- **Caching**: Redis-based caching to reduce API calls and improve response times
- **RESTful API**: Clean REST endpoints for weather data consumption
- **Error Handling**: Comprehensive error handling for various scenarios
- **Temperature Units**: Support for both Celsius and Fahrenheit

## API Endpoints

### 1. Weather Summary
Get locations where temperature will exceed a threshold tomorrow.

```
GET /weather/summary?unit=celsius&temperature=24&locations=2345,1456,7653
```

**Parameters:**
- `unit`: Temperature unit (`celsius` or `fahrenheit`)
- `temperature`: Temperature threshold (integer)
- `locations`: Comma-separated list of OpenWeatherMap location IDs

**Response:**
```json
{
  "locations": [
    {
      "locationId": "2345",
      "locationName": "London",
      "tomorrowTemperature": 26.5,
      "willExceedThreshold": true
    }
  ],
  "unit": "celsius",
  "temperatureThreshold": 24
}
```

### 2. Location Forecast
Get 5-day weather forecast for a specific location.

```
GET /weather/locations/2345?unit=celsius
```

**Parameters:**
- `locationId`: OpenWeatherMap location ID (path parameter)
- `unit`: Temperature unit (optional, defaults to celsius)

**Response:**
```json
{
  "locationId": "2345",
  "locationName": "London",
  "unit": "celsius",
  "forecast": [
    {
      "date": "2025-10-02",
      "temperature": 18.5,
      "description": "partly cloudy",
      "humidity": 65,
      "windSpeed": 4.2
    }
  ]
}
```

## Setup Instructions

### Prerequisites
- Java 17 or higher
- Maven 3.6 or higher
- Redis server (for production use)
- OpenWeatherMap API key

### 1. Get OpenWeatherMap API Key
1. Sign up at [OpenWeatherMap](https://openweathermap.org/api)
2. Get your free API key

### 2. Configuration
Set your API key as an environment variable:
```bash
export OPENWEATHER_API_KEY=your_actual_api_key_here
```

Or update `src/main/resources/application.properties`:
```properties
weather.api.key=your_actual_api_key_here
```

### 3. Redis Setup (Production)
For production deployment, ensure Redis is running:
```bash
# Using Docker
docker run -d -p 6379:6379 redis:alpine

# Or install locally
brew install redis  # macOS
sudo apt-get install redis-server  # Ubuntu
```

### 4. Running the Application

#### Development (Simple Cache)
```bash
mvn spring-boot:run -Dspring.profiles.active=test
```

#### Production (Redis Cache)
```bash
mvn spring-boot:run
```

## Example Usage

### Find locations above 25°C tomorrow
```bash
curl "http://localhost:8080/weather/summary?unit=celsius&temperature=25&locations=2643743,5128581,1850147"
```

### Get 5-day forecast for London (ID: 2643743)
```bash
curl "http://localhost:8080/weather/locations/2643743?unit=fahrenheit"
```

## Rate Limiting

The service implements intelligent rate limiting:
- **Daily Limit**: 10,000 requests to OpenWeatherMap API
- **Caching**: 1-hour cache TTL reduces external API calls
- **Error Handling**: Graceful degradation when limits are exceeded

## Error Responses

The API returns structured error responses:

```json
{
  "error": "RATE_LIMIT_EXCEEDED",
  "message": "Daily rate limit exceeded for OpenWeatherMap API",
  "timestamp": 1696291200000
}
```

**Error Types:**
- `WEATHER_API_ERROR`: Issues with OpenWeatherMap API
- `RATE_LIMIT_EXCEEDED`: Daily rate limit exceeded
- `INVALID_ARGUMENT`: Invalid request parameters
- `INTERNAL_ERROR`: Unexpected server errors

## Location IDs

You can find OpenWeatherMap location IDs by:
1. Using the [OpenWeatherMap city list](https://openweathermap.org/find)
2. Common examples:
   - London, UK: `2643743`
   - New York, US: `5128581`
   - Tokyo, JP: `1850147`
   - Paris, FR: `2988507`

## Architecture

The service follows clean architecture principles:

- **Controllers**: Handle HTTP requests and responses
- **Services**: Business logic and orchestration
- **Clients**: External API integration
- **DTOs**: Data transfer objects for API responses
- **Configuration**: Centralized configuration management
- **Exception Handling**: Global error handling

## Testing

```bash
# Run tests
mvn test

# Run with coverage
mvn clean test jacoco:report
```

## Production Considerations

1. **Monitoring**: Add application monitoring (Micrometer/Prometheus)
2. **Security**: Implement API authentication if needed
3. **Scaling**: Consider horizontal scaling with Redis cluster
4. **Logging**: Configure structured logging for production
5. **Health Checks**: Built-in Spring Boot Actuator endpoints
6. **Rate Limiting**: Monitor and adjust limits based on usage patterns
