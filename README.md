# Weather API Service

A Spring Boot REST API service built with Kotlin that acts as an integration layer between mobile apps and OpenWeatherMap API, featuring intelligent rate limiting, Redis caching, and comprehensive API documentation.

## Features

- **🚀 Kotlin & Spring Boot 3.5.6**: Modern reactive web stack with WebFlux
- **🛡️ Intelligent Rate Limiting**: Ensures compliance with OpenWeatherMap's API limits (10,000 requests/day, 700/hour)
- **⚡ Redis Caching**: 1-hour TTL caching to reduce API calls and improve response times
- **📋 Input Validation**: Comprehensive request validation with detailed error messages
- **📚 OpenAPI Documentation**: Interactive Swagger UI available at `/swagger-ui.html`
- **🐳 Docker Support**: Full containerization with Docker Compose
- **🔧 Flexible Configuration**: Support for multiple deployment profiles
- **📊 Health Checks**: Built-in health monitoring and readiness probes
- **🌡️ Temperature Units**: Support for both Celsius and Fahrenheit

## Technology Stack

- **Language**: Kotlin 1.9.25
- **Framework**: Spring Boot 3.5.6
- **Build Tool**: Maven
- **Caching**: Redis 7
- **Documentation**: SpringDoc OpenAPI 3
- **Containerization**: Docker & Docker Compose
- **Java Version**: 17+

## API Endpoints

### 1. Weather Summary
Get locations where temperature will exceed a threshold tomorrow.

```
GET /weather/summary?unit=celsius&temperature=24&locations=2988507,2643743,2950159,2618425
```

**Parameters:**
- `unit`: Temperature unit (`celsius` or `fahrenheit`) - optional, defaults to `celsius`
- `temperature`: Temperature threshold (integer, -100 to 100) - required
- `locations`: Comma-separated list of OpenWeatherMap location IDs - required

**Response:**
```json
{
  "locations": [
    {
      "locationId": "2643743",
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
GET /weather/locations/2618425?unit=celsius
```

**Parameters:**
- `locationId`: OpenWeatherMap location ID (path parameter) - required
- `unit`: Temperature unit (`celsius` or `fahrenheit`) - optional, defaults to `celsius`

**Response:**
```json
{
  "locationId": "2618425",
  "locationName": "Copenhagen",
  "unit": "celsius",
  "forecast": [
    {
      "date": "2025-10-06",
      "temperature": 18.5,
      "description": "Clear sky",
      "humidity": 65,
      "windSpeed": 4.2
    }
  ]
}
```

## Quick Start

### Prerequisites
- Java 17 or higher
- Maven 3.6 or higher
- OpenWeatherMap API key
- Docker & Docker Compose (for containerized deployment)

### 1. Get OpenWeatherMap API Key
1. Sign up at [OpenWeatherMap](https://openweathermap.org/api)
2. Get your free API key (allows 1,000 calls/day, upgrade available)

### 2. Environment Setup
Set your API key as an environment variable:
```bash
export OPENWEATHER_API_KEY=your_actual_api_key_here
```

### 3. Running the Application

#### Option A: Docker Compose (Recommended)
```bash
# Start the application with Redis
docker-compose up -d

# View logs
docker-compose logs -f weather-api

# Stop services
docker-compose down
```

#### Option B: Local Development (Simple Cache)
```bash
# Run with simple in-memory cache (no Redis required)
mvn spring-boot:run -Dspring-boot.run.profiles=test
```

#### Option C: Local Development (Redis Cache)
```bash
# Start Redis first
docker run -d -p 6379:6379 --name redis redis:7-alpine

# Run application with Redis cache
mvn spring-boot:run
```

The application will be available at:
- **API**: http://localhost:8080
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **API Docs**: http://localhost:8080/api-docs
- **Health Check**: http://localhost:8080/actuator/health

## Configuration

### Application Profiles

- **Default Profile**: Uses Redis cache, suitable for production
- **Test Profile**: Uses simple in-memory cache, suitable for development
- **Docker Profile**: Optimized for containerized deployment

### Configuration Options

Edit `src/main/resources/application.yml`:

```yaml
weather:
  api:
    key: ${OPENWEATHER_API_KEY:your_key_here}
    rate-limit:
      requests-per-day: 10000
      requests-per-hour: 700
  cache:
    ttl: 3600  # Cache TTL in seconds (1 hour)
  client:
    timeout: 5000  # HTTP timeout in milliseconds

spring:
  data:
    redis:
      host: localhost
      port: 6379
      timeout: 2000ms
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

### Using popular location IDs
```bash
# London, UK
curl "http://localhost:8080/weather/locations/2643743"

# New York, US
curl "http://localhost:8080/weather/locations/5128581"

# Tokyo, Japan
curl "http://localhost:8080/weather/locations/1850147"

# Paris, France
curl "http://localhost:8080/weather/locations/2988507"
```

## Rate Limiting & Caching

The service implements intelligent traffic management:

- **Daily Limit**: 10,000 requests to OpenWeatherMap API
- **Hourly Limit**: 700 requests per hour
- **Cache Strategy**: 1-hour TTL reduces external API calls by ~95%
- **Fallback**: In-memory rate limiting when Redis is unavailable
- **Error Handling**: Graceful degradation when limits are exceeded

## API Documentation

Interactive API documentation is available via Swagger UI:
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI Spec**: http://localhost:8080/api-docs

The documentation includes:
- Detailed parameter descriptions
- Example requests and responses
- Interactive "Try it out" functionality
- Response schema definitions

## Error Handling

The API returns structured error responses with appropriate HTTP status codes:

```json
{
  "error": "RATE_LIMIT_EXCEEDED",
  "message": "Daily rate limit exceeded for OpenWeatherMap API",
  "timestamp": 1728172800000
}
```

**Error Types:**
- `VALIDATION_ERROR`: Invalid request parameters (400)
- `LOCATION_NOT_FOUND`: Invalid location ID (404)
- `WEATHER_API_ERROR`: Issues with OpenWeatherMap API (502)
- `RATE_LIMIT_EXCEEDED`: Daily/hourly rate limit exceeded (429)
- `SERVICE_UNAVAILABLE`: Temporary service issues (503)

## Development

### Building the Application
```bash
# Clean and compile
mvn clean compile

# Run tests
mvn test

# Package JAR
mvn package

# Skip tests during build
mvn package -DskipTests
```

### Docker Build
```bash
# Build Docker image
docker build -t weather-api .

# Run with custom environment
docker run -p 8080:8080 \
  -e OPENWEATHER_API_KEY=your_key \
  weather-api
```

### Health Monitoring

The application includes built-in health checks:
- **Liveness**: http://localhost:8080/actuator/health
- **Custom checks**: Redis connectivity, API rate limit status

## Production Deployment

### Environment Variables
```bash
# Required
OPENWEATHER_API_KEY=your_actual_api_key

# Redis Configuration (if using external Redis)
SPRING_DATA_REDIS_HOST=your_redis_host
SPRING_DATA_REDIS_PORT=6379
SPRING_DATA_REDIS_PASSWORD=your_redis_password

# Optional
SPRING_PROFILES_ACTIVE=docker
SERVER_PORT=8080
```

### Docker Compose Production
```yaml
version: '3.8'
services:
  weather-api:
    image: weather-api:latest
    environment:
      - OPENWEATHER_API_KEY=${OPENWEATHER_API_KEY}
      - SPRING_PROFILES_ACTIVE=docker
    ports:
      - "8080:8080"
    depends_on:
      - redis
    restart: unless-stopped
    
  redis:
    image: redis:7-alpine
    volumes:
      - redis_data:/data
    restart: unless-stopped
```

## Monitoring & Observability

- **Health Checks**: Built-in Spring Boot Actuator endpoints
- **Logging**: Structured logging with request/response tracking
- **Metrics**: Request count, response times, cache hit rates
- **Error Tracking**: Comprehensive error logging with stack traces

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Make your changes
4. Run tests (`mvn test`)
5. Commit your changes (`git commit -m 'Add amazing feature'`)
6. Push to the branch (`git push origin feature/amazing-feature`)
7. Open a Pull Request

## License

This project is licensed under the MIT License - see the LICENSE file for details.
