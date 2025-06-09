# User Subscription Status Check

## Objective
Add functionality to check whether a user currently has an active subscription.

## Requirements

### API Endpoint
Add a new endpoint to check if a user is currently subscribed:
```
GET /users/{userId}/subscription-status
```

### Response Format
```json
{
  "userId": "string",
  "isSubscribed": boolean,
  "activeSubscriptions": [
    {
      "id": "uuid",
      "productId": "monthly|annual",
      "startDate": "2024-01-01T00:00:00Z",
      "endDate": "2024-02-01T00:00:00Z",
      "status": "active"
    }
  ],
  "subscriptionCount": number
}
```

### Business Logic
- A user is considered "subscribed" if they have at least one active subscription
- Active subscriptions are those with:
  - `status = "active"`
  - Current date is between `startDate` and `endDate`
- Return all currently active subscriptions for the user
- Include total count of active subscriptions

### Technical Implementation
- Add new route in `OrderRoutes.scala`
- Add new method in `OrderService.scala` trait and implementation
- Add new query method in `Store.scala` trait and `PostgresStore.scala`
- Follow existing patterns for error handling and JSON encoding/decoding

## Acceptance Criteria
- [x] New GET endpoint `/users/{userId}/subscription-status` implemented
- [x] Endpoint returns correct subscription status for users with active subscriptions
- [x] Endpoint returns correct status for users with no subscriptions
- [x] Endpoint returns correct status for users with only expired/cancelled subscriptions
- [x] Proper error handling for invalid user IDs
- [x] JSON response matches specified format
- [x] Unit tests for service layer logic
- [x] Integration tests for the endpoint
- [x] Database query efficiently filters by date and status
- [x] Code follows existing project patterns and style
- [x] All existing tests still pass
- [x] `sbt commitCheck` passes

## Test Scenarios
1. **User with active monthly subscription** - should return `isSubscribed: true`
2. **User with active annual subscription** - should return `isSubscribed: true`
3. **User with multiple active subscriptions** - should return all active ones
4. **User with expired subscriptions only** - should return `isSubscribed: false`
5. **User with cancelled subscriptions only** - should return `isSubscribed: false`
6. **User with no subscriptions** - should return `isSubscribed: false`
7. **Non-existent user** - should return appropriate error response

## Sample Usage
```bash
# Check subscription status for a user
curl http://localhost:8080/users/user123/subscription-status

# Expected response for subscribed user:
{
  "userId": "user123",
  "isSubscribed": true,
  "activeSubscriptions": [
    {
      "id": "abc-123",
      "productId": "monthly",
      "startDate": "2024-01-15T00:00:00Z",
      "endDate": "2024-02-15T00:00:00Z",
      "status": "active"
    }
  ],
  "subscriptionCount": 1
}
```

## Dependencies
- Requires existing subscription and user management functionality
- No external dependencies needed

## Status
🟢 **Complete** - Feature implemented with full test coverage
