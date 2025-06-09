# Logging Context Wrapper

## Overview

Introduce a standardized logging context wrapper to ensure consistency across all structured logging calls. Currently, logging contexts are manually created as `Map[String, String]` throughout the codebase, leading to potential inconsistencies in key naming, value formatting, and context propagation.

## Current State

The codebase uses structured logging with manual Map creation:

```scala
// Current approach - manual Map creation
logger.info(
  Map(
    "operation" -> "createOrder",
    "userId" -> request.userId,
    "productId" -> request.productId,
    "correlationId" -> correlationId.value
  )
)("Creating order")

// Inconsistent formatting
logger.error(
  Map(
    "operation" -> "handleServiceError", 
    "correlationId" -> correlationId.value,
    "orderId" -> orderId.value.toString  // .toString sometimes present
  ), 
  t
)("Failed to process order")
```

## Proposed Solution

Create a `LoggingContext` abstraction that provides:

1. **Consistent key naming and value formatting**
2. **Builder pattern for context construction**
3. **Automatic correlation ID propagation**
4. **Type-safe context creation**

### Implementation Design

```scala
case class LoggingContext private (context: Map[String, String]):
  def withOperation(operation: String): LoggingContext = 
    copy(context = context + ("operation" -> operation))
  
  def withCorrelationId(correlationId: CorrelationId): LoggingContext =
    copy(context = context + ("correlationId" -> correlationId.value))
  
  def withUserId(userId: UserId): LoggingContext =
    copy(context = context + ("userId" -> userId.value))
  
  def withOrderId(orderId: OrderId): LoggingContext =
    copy(context = context + ("orderId" -> orderId.value.toString))
  
  def withProductId(productId: ProductId): LoggingContext =
    copy(context = context + ("productId" -> productId.value))
  
  def withCustom(key: String, value: String): LoggingContext =
    copy(context = context + (key -> value))
  
  def toMap: Map[String, String] = context

object LoggingContext:
  def empty: LoggingContext = LoggingContext(Map.empty)
  
  def withOperation(operation: String): LoggingContext = 
    empty.withOperation(operation)
```

### Enhanced Logger Extension

```scala
extension [F[_]](logger: SelfAwareStructuredLogger[F])
  def infoWithContext(ctx: LoggingContext)(message: => String): F[Unit] =
    logger.info(ctx.toMap)(message)
  
  def errorWithContext(ctx: LoggingContext, throwable: Throwable)(message: => String): F[Unit] =
    logger.error(ctx.toMap, throwable)(message)
  
  def warnWithContext(ctx: LoggingContext)(message: => String): F[Unit] =
    logger.warn(ctx.toMap)(message)
```

### Usage Examples

```scala
// Service layer
val baseContext = LoggingContext
  .withOperation("createOrder")
  .withUserId(request.userId)
  .withProductId(request.productId)

logger.infoWithContext(baseContext)("Creating order")

// Add order ID after creation
val enrichedContext = baseContext.withOrderId(orderId)
logger.infoWithContext(enrichedContext)("Order created successfully")

// HTTP routes with correlation ID
val httpContext = LoggingContext
  .withOperation("POST /orders")
  .withCorrelationId(correlationId)
  .withCustom("method", "POST")
  .withCustom("uri", uri.toString)

logger.infoWithContext(httpContext)(s"HTTP Request: POST ${uri.path}")
```

## Benefits

1. **Consistency**: Standardized key names and value formatting
2. **Type Safety**: Compile-time checking of context keys  
3. **Maintainability**: Centralized context logic, easier to modify
4. **Readability**: Fluent builder API improves code clarity
5. **Extensibility**: Easy to add new context fields

## Implementation Plan

1. Create `LoggingContext` case class with builder methods
2. Add logger extensions for context-aware logging
3. Update `OrderService` to use new logging context
4. Update `OrderRoutes` to use new logging context  
5. Update `DatabaseMigration` to use new logging context
6. Add utility methods for common context patterns

## Acceptance Criteria

- [ ] Create `LoggingContext` case class with builder pattern
- [ ] Implement type-safe methods for all domain entities (UserId, OrderId, etc.)
- [ ] Add logger extension methods for context-aware logging
- [ ] Update `OrderService` to use `LoggingContext` throughout
- [ ] Update `OrderRoutes` to use `LoggingContext` with correlation IDs
- [ ] Update `DatabaseMigration` to use `LoggingContext`
- [ ] Ensure consistent value formatting across all context fields
- [ ] All existing tests continue to pass
- [ ] Add unit tests for `LoggingContext` builder methods
- [ ] Verify log output maintains same structured format

## Status

🔴 **Pending** - Awaiting implementation of standardized logging context wrapper