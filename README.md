# Order Management System

[![CI](https://github.com/channingwalton/orders/actions/workflows/ci.yml/badge.svg)](https://github.com/channingwalton/orders/actions/workflows/ci.yml)

This project has been written entirely by Claude Code. I haven't included the transcript, I should have kept it, sorry, but it was mostly telling claude to read the prompt file and make sure tests pass before committing and pushing to GH.

See the prompts directory for where the prompts claude worked from are kept.

----

A Scala-based order management system for streaming services, built with http4s, doobie, and PostgreSQL.

## Overview

This system manages orders and subscriptions for streaming services. When an order is created, a corresponding subscription is automatically generated with the appropriate duration based on the product type.

## Features

- **Order Management**: Create, retrieve, and cancel orders with detailed cancellation tracking
- **Subscription Management**: Automatic subscription creation and management with status tracking
- **Product Types**: Monthly and annual subscription products
- **Advanced Cancellation**: Support for immediate or end-of-period cancellations with reasons
- **User Status Tracking**: Get user subscription status and cancellation details
- **Database Integration**: PostgreSQL with Flyway migrations and timestamp precision handling
- **Structured Logging**: Comprehensive logging with correlation IDs and context tracking
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

### Get User Subscription Status
```
GET /users/{userId}/subscription-status
```

**Response**: Returns the user's subscription status including active subscriptions count.

### Cancel Order (Simple)
```
PUT /orders/{orderId}/cancel
```

**Response**: Returns 204 No Content on success.

### Cancel Order (Advanced)
```
PUT /orders/{orderId}/cancel
Content-Type: application/json

{
  "reason": "UserRequest",
  "cancellationType": "Immediate",
  "notes": "Customer requested cancellation"
}
```

**Response**: Returns 204 No Content on success.

### Get Order Cancellation Details
```
GET /orders/{orderId}/cancellation
```

**Response**: Returns cancellation details or 404 if order was not cancelled.

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
  "cancelledAt": "2024-01-15T00:00:00Z",
  "effectiveEndDate": "2024-02-01T00:00:00Z",
  "createdAt": "2024-01-01T00:00:00Z",
  "updatedAt": "2024-01-01T00:00:00Z"
}
```

### UserSubscriptionStatus
```json
{
  "userId": "string",
  "isSubscribed": true,
  "activeSubscriptions": [...],
  "subscriptionCount": 2
}
```

### OrderCancellation
```json
{
  "id": "uuid",
  "orderId": "uuid",
  "reason": "UserRequest|PaymentFailure|Violation|Other",
  "cancellationType": "Immediate|EndOfPeriod",
  "notes": "Optional cancellation notes",
  "cancelledAt": "2024-01-15T00:00:00Z",
  "cancelledBy": "User|System|Admin",
  "effectiveDate": "2024-01-15T00:00:00Z",
  "createdAt": "2024-01-15T00:00:00Z",
  "updatedAt": "2024-01-15T00:00:00Z"
}
```

## Technology Stack

- **Scala 3.3.6**: Main programming language
- **http4s**: HTTP server and client
- **doobie**: Database access library
- **PostgreSQL**: Database
- **Flyway**: Database migrations
- **Circe**: JSON encoding/decoding
- **log4cats**: Structured logging with correlation IDs
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

### Architecture

The application follows a layered architecture:

1. **HTTP Routes**: Handle HTTP requests/responses using http4s
2. **Service Layer**: Business logic and orchestration
3. **Store Layer**: Database access abstraction using doobie

This design enables easy testing with mocks and clear separation of concerns.

### Development Workflow

For development, use the `commitCheck` alias to run all checks before committing:

```bash
sbt commitCheck
```

This runs:
- `clean`: Clean build artifacts
- `compile`: Compile the code
- `test`: Run all tests

## Running Locally

For local development and testing, we provide shell scripts to simplify the setup:

### Quick Start

1. **Start PostgreSQL in Docker:**
   ```bash
   ./start-postgres.sh
   ```
   This will start a PostgreSQL container with the correct database, user, and password configured.

2. **Run the application:**
   ```bash
   ./run-app.sh
   ```
   This will start the orders service on `http://localhost:8080`.

3. **Stop PostgreSQL when done:**
   ```bash
   ./stop-postgres.sh
   ```

### Sample API Usage

Once the application is running, you can interact with it using curl:

#### Create an Order
```bash
curl -X POST http://localhost:8080/orders \
  -H "Content-Type: application/json" \
  -d '{"userId": "user123", "productId": "monthly"}'
```

#### Get an Order
```bash
# Replace {orderId} with the ID from the create response
curl http://localhost:8080/orders/{orderId}
```

#### List User Orders
```bash
curl http://localhost:8080/users/user123/orders
```

#### List User Subscriptions
```bash
curl http://localhost:8080/users/user123/subscriptions
```

#### Get User Subscription Status
```bash
curl http://localhost:8080/users/user123/subscription-status
```

#### Cancel an Order (Simple)
```bash
# Replace {orderId} with an actual order ID
curl -X PUT http://localhost:8080/orders/{orderId}/cancel
```

#### Cancel an Order (Advanced)
```bash
curl -X PUT http://localhost:8080/orders/{orderId}/cancel \
  -H "Content-Type: application/json" \
  -d '{"reason": "UserRequest", "cancellationType": "EndOfPeriod", "notes": "Customer wants to finish current period"}'
```

#### Get Order Cancellation Details
```bash
curl http://localhost:8080/orders/{orderId}/cancellation
```

#### Example Complete Workflow
```bash
# 1. Create a monthly order
ORDER_RESPONSE=$(curl -s -X POST http://localhost:8080/orders \
  -H "Content-Type: application/json" \
  -d '{"userId": "user123", "productId": "monthly"}')

echo "Created order: $ORDER_RESPONSE"

# 2. Extract order ID (requires jq)
ORDER_ID=$(echo $ORDER_RESPONSE | jq -r '.id')

# 3. Get the order
curl http://localhost:8080/orders/$ORDER_ID

# 4. Check user's subscriptions
curl http://localhost:8080/users/user123/subscriptions

# 5. Cancel the order
curl -X PUT http://localhost:8080/orders/$ORDER_ID/cancel
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Run tests: `sbt commitCheck`
5. Submit a pull request

## License

This project is licensed under the MIT License.
