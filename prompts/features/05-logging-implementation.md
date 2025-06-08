# Logging Implementation

## Objective
Implement comprehensive logging throughout the application to track user interactions and capture all errors for debugging and monitoring purposes.

## Requirements

### Current Problem
- No structured logging in place for user interactions
- Error scenarios lack detailed logging for debugging
- No audit trail for order and subscription operations
- Difficult to troubleshoot issues in production
- No visibility into application usage patterns

### Core Functionality

#### 1. User Interaction Logging
Log all API requests and responses:
- HTTP method, URI, and request parameters
- User identification (when available)
- Response status codes and timing
- Request/response payload sizes
- Client information (user agents, IP addresses)

#### 2. Business Logic Logging
Log key business operations:
- Order creation, cancellation, and status changes
- Subscription lifecycle events (creation, cancellation, status changes)
- Payment processing events
- User authentication and authorization events

#### 3. Error Logging
Comprehensive error capture:
- All ServiceError exceptions with full context
- Database operation failures
- HTTP request/response errors
- Unexpected system errors with stack traces
- Validation failures and their causes

#### 4. Structured Logging Format
Use structured logging with consistent fields:
- Timestamp with timezone
- Log level (DEBUG, INFO, WARN, ERROR)
- Component/service identifier
- Correlation IDs for request tracing
- Contextual metadata (user ID, order ID, etc.)

### Technical Implementation

#### Logging Framework
Use SLF4J with Logback and [log4cats](https://typelevel.org/log4cats/)

```
libraryDependencies ++= Seq(
  "org.typelevel" %% "log4cats-core"    % "2.7.1",  // Only if you want to Support Any Backend
  "org.typelevel" %% "log4cats-slf4j"   % "2.7.1",  // Direct Slf4j Support - Recommended
)
```

```scala
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import cats.effect.*
import cats.syntax.all.*  

object MyThing {
  // Impure But What 90% of Folks I know do with log4s
  implicit def logger[F[_]: Sync]: Logger[F] = Slf4jLogger.getLogger[F]

  // Arbitrary Local Function Declaration
  def doSomething[F[_]: Sync]: F[Unit] =
    Logger[F].info("Logging Start Something") *>
            Sync[F].delay(println("I could be doing anything"))
                    .attempt.flatMap {
                      case Left(e) => Logger[F].error(e)("Something Went Wrong")
                      case Right(_) => Sync[F].pure(())
                    }
}

def safelyDoThings[F[_]: Sync]: F[Unit] = for {
  logger <- Slf4jLogger.create[F]
  _ <- logger.info("Logging at start of safelyDoThings")
  something <- Sync[F].delay(println("I could do anything"))
          .onError { case e => logger.error(e)("Something Went Wrong in safelyDoThings") }
  _ <- logger.info("Logging at end of safelyDoThings")
} yield something

def passForEasierUse[F[_]: Sync : Logger] = for {
  _ <- Logger[F].info("Logging at start of passForEasierUse")
  something <- Sync[F].delay(println("I could do anything"))
          .onError { case e => Logger[F].error(e)("Something Went Wrong in passForEasierUse") }
  _ <- Logger[F].info("Logging at end of passForEasierUse")
} yield something
```

#### Service Layer Logging
Add logging to OrderService operations:
- Log order creation with user and product details
- Log cancellation requests with reasons
- Log subscription status changes
- Log error conditions with full context

#### Database Layer Logging
Log database operations:
- Query execution timing
- Connection pool metrics
- Transaction boundaries
- Database errors and retry attempts

## Acceptance Criteria

### Request/Response Logging
- [ ] All HTTP requests logged with method, URI, headers, and timing
- [ ] All HTTP responses logged with status code and response size
- [ ] Request correlation IDs generated and propagated
- [ ] Sensitive data (passwords, tokens) excluded from logs

### Business Operation Logging
- [ ] Order creation logged with user ID, product ID, and timestamp
- [ ] Order cancellation logged with reason and cancellation type
- [ ] Subscription changes logged with old/new status
- [ ] User authentication events logged

### Error Logging
- [ ] All ServiceError exceptions logged with full context
- [ ] Database errors logged with query details (sanitized)
- [ ] HTTP client errors logged with request/response details
- [ ] Unexpected errors logged with stack traces

### Structured Format
- [ ] All logs use consistent JSON structure
- [ ] Timestamps include timezone information
- [ ] Log levels used appropriately (DEBUG, INFO, WARN, ERROR)
- [ ] Contextual fields included (user ID, order ID, correlation ID)

### Performance
- [ ] Logging overhead does not impact application performance
- [ ] Async logging used where appropriate
- [ ] Log levels configurable via environment variables
- [ ] Log rotation and retention policies implemented

## Test Scenarios

### Request Logging
1. **API Requests**: All endpoints log request/response details
2. **Error Responses**: Error scenarios include appropriate log entries
3. **Timing**: Request duration logging works correctly
4. **Correlation**: Request IDs propagate through the entire request lifecycle

### Business Logic Logging
1. **Order Operations**: Create, update, cancel operations logged
2. **Subscription Events**: Status changes and lifecycle events logged
3. **User Actions**: Authentication and authorization events logged

### Error Scenarios
1. **Service Errors**: All ServiceError types logged with context
2. **Database Failures**: Connection and query errors logged
3. **Validation Errors**: Input validation failures logged with details
4. **System Errors**: Unexpected exceptions logged with stack traces

### Log Format Testing
1. **JSON Structure**: All logs produce valid JSON
2. **Field Consistency**: Required fields present in all log entries
3. **Sensitive Data**: No passwords or tokens in log output
4. **Timezone**: Timestamps include correct timezone information

## Implementation Notes

### Logging Configuration
Configure different log levels for different environments:
- **Development**: DEBUG level with console output
- **Testing**: INFO level with file output
- **Production**: WARN level with JSON output to centralized logging

### Correlation IDs
Generate unique correlation IDs for request tracing:
```scala
case class CorrelationId(value: String) extends AnyVal
object CorrelationId:
  def generate: CorrelationId = CorrelationId(UUID.randomUUID().toString)
```

### Sensitive Data Protection
Exclude sensitive information from logs:
- User passwords and authentication tokens
- Credit card numbers and payment details
- Personal identification information
- Database connection strings and credentials

### Performance Considerations
- Use async logging appenders to minimize performance impact
- Implement log sampling for high-volume debug logs
- Configure appropriate buffer sizes and flush intervals
- Monitor logging overhead in production

### Monitoring Integration
Structure logs for easy integration with monitoring tools:
- Use consistent field names across all components
- Include metrics data in log entries where appropriate
- Support log aggregation and analysis tools
- Enable alerting on error patterns

## Future Considerations
- Integration with distributed tracing systems (Jaeger, Zipkin)
- Metrics collection and export (Prometheus)
- Log-based alerting and monitoring dashboards
- Compliance requirements for log retention and privacy

## Status
🔴 **Pending** - Ready for implementation
