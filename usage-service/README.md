# Usage Service

## Description

The Usage Service is a Spring Boot application that processes energy messages from a RabbitMQ queue and maintains energy usage records in the database.

## Responsibilities

1. **Receives Messages**: Listens to the `energy.messages` queue for PRODUCER and USER messages
2. **Updates Database**: Aggregates kWh values by hour and stores them in the `energy_usage` table
3. **Distributes Energy**: 
   - PRODUCER messages add to `community_produced`
   - USER messages add to `community_used` (up to available community production)
   - Excess user demand is served from the grid (`grid_used`)
4. **Publishes Updates**: Sends UPDATE messages to the `energy.updates` queue after processing

## Architecture

- **Message Format**: JSON-serialized EnergyMessage objects
- **Database**: PostgreSQL with JPA/Hibernate ORM
- **Queue Names**:
  - Input: `energy.messages`
  - Output: `energy.updates`

## Database Schema

```sql
CREATE TABLE energy_usage (
    hour VARCHAR(19) PRIMARY KEY,
    community_produced DOUBLE PRECISION,
    community_used DOUBLE PRECISION,
    grid_used DOUBLE PRECISION
);
```

## Configuration

See `application.properties` for RabbitMQ and database connection settings.

## Running

```bash
./mvnw spring-boot:run
```

Prerequisites:
- PostgreSQL database running on localhost:5432
- RabbitMQ broker running on localhost:5672

