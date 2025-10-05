# Docker Quick Start Guide

## Prerequisites
- Docker and Docker Compose installed
- OpenWeatherMap API key (get free key from https://openweathermap.org/api)

## Quick Start

1. **Set up environment variables:**
   ```bash
   cp .env.example .env
   # Edit .env file with your OpenWeatherMap API key
   ```

2. **Build and run the application:**
   ```bash
   # Build and start all services
   docker-compose up --build

   # Or run in background
   docker-compose up --build -d
   ```

3. **Access the application:**
   - API: http://localhost:8080
   - Swagger UI: http://localhost:8080/swagger-ui.html
   - Health Check: http://localhost:8080/actuator/health

## Docker Commands

```bash
# Build the application
docker-compose build

# Start services
docker-compose up -d

# Stop services
docker-compose down

# View logs
docker-compose logs -f weather-api

# Rebuild and restart
docker-compose up --build --force-recreate

# Clean up everything (including volumes)
docker-compose down -v
```

## Services

- **weather-api**: Main Spring Boot application (port 8080)
- **redis**: Redis cache for rate limiting and weather data caching (port 6379)

## Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `OPENWEATHER_API_KEY` | OpenWeatherMap API key | Demo key (limited) |
| `SPRING_DATA_REDIS_HOST` | Redis hostname | redis |
| `SPRING_DATA_REDIS_PORT` | Redis port | 6379 |

## Health Monitoring

Both services include health checks:
- Weather API: Uses Spring Boot Actuator
- Redis: Uses redis-cli ping

Check service status:
```bash
docker-compose ps
```

## Development

For development with live reload, you can override the docker-compose configuration:

Create `docker-compose.override.yml`:
```yaml
version: '3.8'
services:
  weather-api:
    volumes:
      - ./src:/app/src
    environment:
      - SPRING_DEVTOOLS_RESTART_ENABLED=true
```
