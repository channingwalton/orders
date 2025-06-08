# Order Management System

[![CI](https://github.com/channingwalton/orders/actions/workflows/ci.yml/badge.svg)](https://github.com/channingwalton/orders/actions/workflows/ci.yml)

A Scala-based order management system for streaming services, built with http4s, doobie, and PostgreSQL.

## Overview

This system manages orders and subscriptions for streaming services. When an order is created, a corresponding subscription is automatically generated with the appropriate duration based on the product type.

## Features

- **Order Management**: Create, retrieve, and cancel orders
- **Subscription Management**: Automatic subscription creation and management
- **Product Types**: Monthly and annual subscription products
- **Database Integration**: PostgreSQL with Flyway migrations
- **Comprehensive Testing**: Unit tests with testcontainers for database integration

## API Endpoints

### Create Order
```
POST /orders
Content-Type: application/json

{
  "userId": "user123",
  "productId": "monthly"
}
```

**Response**: Returns the created order with generated ID and timestamps.

### Get Order
```
GET /orders/{orderId}
```

**Response**: Returns the order details or 404 if not found.

### List User Orders
```
GET /users/{userId}/orders
```

**Response**: Returns a list of all orders for the specified user.

### List User Subscriptions
```
GET /users/{userId}/subscriptions
```

**Response**: Returns a list of all subscriptions for the specified user.

### Cancel Order
```
PUT /orders/{orderId}/cancel
```

**Response**: Returns 204 No Content on success.

## Product Types

- **monthly**: Creates a 1-month subscription
- **annual**: Creates a 1-year subscription

## Data Models

### Order
```json
{
  "id": "uuid",
  "userId": "string",
  "productId": "monthly|annual",
  "status": "active|cancelled",
  "createdAt": "2024-01-01T00:00:00Z",
  "updatedAt": "2024-01-01T00:00:00Z"
}
```

### Subscription
```json
{
  "id": "uuid",
  "orderId": "uuid",
  "userId": "string",
  "productId": "monthly|annual",
  "startDate": "2024-01-01T00:00:00Z",
  "endDate": "2024-02-01T00:00:00Z",
  "status": "active|expired|cancelled",
  "createdAt": "2024-01-01T00:00:00Z",
  "updatedAt": "2024-01-01T00:00:00Z"
}
```

## Technology Stack

- **Scala 3.3.6**: Main programming language
- **http4s**: HTTP server and client
- **doobie**: Database access library
- **PostgreSQL**: Database
- **Flyway**: Database migrations
- **Circe**: JSON encoding/decoding
- **munit**: Testing framework
- **testcontainers**: Integration testing with Docker

## Getting Started

### Prerequisites

- Java 21
- SBT 1.11.1
- Docker (for running tests)
- PostgreSQL (for running the application)

### Database Setup

1. Create a PostgreSQL database:
```sql
CREATE DATABASE orders;
CREATE USER orders WITH PASSWORD 'orders';
GRANT ALL PRIVILEGES ON DATABASE orders TO orders;
```

2. Set environment variables:
```bash
export DATABASE_URL=jdbc:postgresql://localhost:5432/orders
export DATABASE_USER=orders
export DATABASE_PASSWORD=orders
```

### Running the Application

1. Compile the project:
```bash
sbt compile
```

2. Run tests:
```bash
sbt test
```

3. Run the application:
```bash
sbt run
```

The server will start on `http://localhost:8080` by default.

### Configuration

The application can be configured using environment variables:

- `HTTP_HOST`: Server host (default: 0.0.0.0)
- `HTTP_PORT`: Server port (default: 8080)
- `DATABASE_URL`: PostgreSQL connection URL
- `DATABASE_USER`: Database username
- `DATABASE_PASSWORD`: Database password
- `DATABASE_MAX_POOL_SIZE`: Connection pool size (default: 10)

## Development

### Project Structure

```
src/
├── main/
│   ├── resources/
│   │   ├── application.conf
│   │   ├── logback.xml
│   │   └── db/migration/
│   └── scala/acme/orders/
│       ├── Main.scala
│       ├── OrderService.scala
│       ├── config/
│       ├── db/
│       ├── models/
│       └── routes/
└── test/
    └── scala/acme/orders/
        ├── OrderServiceTest.scala
        ├── db/PostgresStoreTest.scala
        └── routes/OrderRoutesTest.scala
```

### Architecture

The application follows a layered architecture:

1. **HTTP Routes**: Handle HTTP requests/responses using http4s
2. **Service Layer**: Business logic and orchestration
3. **Store Layer**: Database access abstraction using doobie

This design enables easy testing with mocks and clear separation of concerns.

### Running CI Pipeline

The project includes a CI alias for running all checks:

```bash
sbt ci
```

This runs:
- `clean`: Clean build artifacts
- `compile`: Compile the code
- `test`: Run all tests

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Run tests: `sbt ci`
5. Submit a pull request

## License

This project is licensed under the MIT License.
