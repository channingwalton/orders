# Error Mapping in OrderRoutes

## Objective
Map ServiceError exceptions from OrderService to appropriate HTTP status codes in OrderRoutes to provide meaningful error responses to clients instead of generic 500 errors.

## Requirements

### Current Problem
- OrderRoutes currently don't handle ServiceError exceptions
- ServiceError exceptions result in 500 Internal Server Error responses regardless of the actual error type
- No structured error response format for API consumers
- Poor debugging experience for client applications

### Core Functionality

#### 1. Error Mapping Strategy
Map ServiceError types to appropriate HTTP status codes:
- `OrderNotFound` → 404 Not Found
- `InvalidProduct` → 400 Bad Request  
- `OrderAlreadyCancelled` → 409 Conflict
- `DatabaseError` → 500 Internal Server Error

#### 2. Error Response Format
Return consistent error responses with:
- Appropriate HTTP status code
- Error message from ServiceError
- Consistent response structure

#### 3. Route Coverage
Apply error handling to all routes that use OrderService:
- POST /orders (create order)
- GET /orders/{orderId} (get order)
- GET /users/{userId}/orders (get user orders)
- GET /users/{userId}/subscriptions (get user subscriptions)
- GET /users/{userId}/subscription-status (get subscription status)
- PUT /orders/{orderId}/cancel (cancel order)
- GET /orders/{orderId}/cancellation (get cancellation)

### Technical Implementation

#### Error Handler Function
```scala
def handleServiceError(error: ServiceError): F[Response[F]] = error match
  case ServiceError.OrderNotFound(_) => NotFound(error.getMessage)
  case ServiceError.InvalidProduct(_) => BadRequest(error.getMessage)
  case ServiceError.OrderAlreadyCancelled(_) => Conflict(error.getMessage)
  case ServiceError.DatabaseError(_) => InternalServerError("Internal server error")
```

#### Route Error Handling
Wrap service calls with `.handleErrorWith` or `.recoverWith` to catch ServiceError exceptions.

## Acceptance Criteria

### Error Mapping
- [ ] OrderNotFound errors return 404 Not Found status
- [ ] InvalidProduct errors return 400 Bad Request status
- [ ] OrderAlreadyCancelled errors return 409 Conflict status
- [ ] DatabaseError errors return 500 Internal Server Error status

### Response Format
- [ ] Error responses include the error message in the body
- [ ] Error responses have correct Content-Type headers
- [ ] Error message content matches ServiceError.getMessage

### Route Coverage
- [ ] All OrderService method calls are wrapped with error handling
- [ ] Successful operations continue to work unchanged
- [ ] No regression in existing functionality

### Error Content
- [ ] 404 responses include specific "Order not found" message with order ID
- [ ] 400 responses include specific "Invalid product" message with product ID
- [ ] 409 responses include specific "Order already cancelled" message with order ID
- [ ] 500 responses use generic "Internal server error" message (no DB details exposed)

## Test Scenarios

### Error Response Testing
1. **OrderNotFound**: Request non-existent order ID → 404 with message
2. **InvalidProduct**: Create order with invalid product → 400 with message
3. **OrderAlreadyCancelled**: Cancel already cancelled order → 409 with message
4. **DatabaseError**: Simulate DB error → 500 with generic message

### Route Coverage Testing
1. **GET /orders/{orderId}**: Test OrderNotFound scenario
2. **POST /orders**: Test InvalidProduct scenario
3. **PUT /orders/{orderId}/cancel**: Test OrderNotFound and OrderAlreadyCancelled scenarios
4. **All routes**: Verify successful operations still work

### Message Content Testing
1. **Error Messages**: Verify each error type returns expected message format
2. **No Information Leakage**: Ensure DatabaseError doesn't expose internal details
3. **Consistent Format**: All error responses follow same structure

## Implementation Notes

### Error Recovery Pattern
Use cats-effect error handling patterns:
```scala
orderService.someMethod(params)
  .handleErrorWith {
    case error: ServiceError => handleServiceError(error)
    case other => InternalServerError("Unexpected error")
  }
```

### Security Considerations
- DatabaseError responses should not expose internal database details
- Error messages should be safe for external consumption
- No sensitive information in error responses

### Performance Considerations
- Error handling should not impact successful request performance
- Error responses should be generated efficiently
- No additional database calls for error formatting

## Implementation Results

### Error Mapping Implementation
- ✅ Added `handleServiceError` function in OrderRoutes that maps ServiceError types to HTTP status codes
- ✅ OrderNotFound → 404 Not Found with error message
- ✅ InvalidProduct → 400 Bad Request with error message  
- ✅ OrderAlreadyCancelled → 409 Conflict with error message
- ✅ DatabaseError → 500 Internal Server Error with generic message (no DB details exposed)

### Route Coverage
- ✅ All OrderService method calls wrapped with `.handleErrorWith` error recovery
- ✅ POST /orders (create order) - handles InvalidProduct errors
- ✅ GET /orders/{orderId} (get order) - handles OrderNotFound errors
- ✅ GET /users/{userId}/orders (get user orders) - error handling added
- ✅ GET /users/{userId}/subscriptions (get user subscriptions) - error handling added
- ✅ GET /users/{userId}/subscription-status (get subscription status) - error handling added
- ✅ PUT /orders/{orderId}/cancel (cancel order) - handles OrderNotFound and OrderAlreadyCancelled errors
- ✅ GET /orders/{orderId}/cancellation (get cancellation) - error handling added

### Test Coverage
- ✅ Added test for 400 Bad Request when creating order with invalid product
- ✅ Added test for 404 Not Found when getting non-existent order
- ✅ Added test for 409 Conflict when cancelling already cancelled order
- ✅ All existing tests continue to pass (36 total tests passing)
- ✅ Error responses include proper error messages in response body

### Technical Implementation
- Error handling uses cats-effect `.handleErrorWith` pattern
- Generic "Internal server error" message for DatabaseError to prevent information leakage
- Successful operations remain unchanged with no performance impact
- Error responses generated efficiently without additional database calls

## Status
🟢 **Complete** - Successfully implemented and tested