# PostgreSQL Test Optimization

## Objective
Optimize PostgresStore tests by reducing unnecessary database commits and improving test performance.

## Current Issue
The current PostgresStore tests commit every single database operation separately, which is inefficient and doesn't reflect real-world usage patterns. Multiple related operations should be grouped within a single transaction.

## Current Pattern (Inefficient)
```scala
for
  _ <- store.commit(store.createOrder(order1))
  _ <- store.commit(store.createOrder(order2))
  _ <- store.commit(store.createOrder(order3))
  _ <- store.commit(store.createSubscription(subscription1))
  _ <- store.commit(store.createSubscription(subscription2))
  _ <- store.commit(store.createSubscription(subscription3))
  
  user1Orders <- store.commit(store.findOrdersByUser(user1))
  user2Orders <- store.commit(store.findOrdersByUser(user2))
yield // assertions
```

## Improved Pattern (Efficient)
```scala
for
  // Setup data in a single transaction
  _ <- store.commit(
    store.createOrder(order1) *>
    store.createOrder(order2) *>
    store.createOrder(order3) *>
    store.createSubscription(subscription1) *>
    store.createSubscription(subscription2) *>
    store.createSubscription(subscription3)
  )
  
  // Query data (can be separate commits for clarity)
  user1Orders <- store.commit(store.findOrdersByUser(user1))
  user2Orders <- store.commit(store.findOrdersByUser(user2))
yield // assertions
```

## Benefits of Optimization

### Performance
- Fewer database round trips
- Reduced transaction overhead
- Faster test execution
- Better resource utilization

### Realism
- More closely matches production usage patterns
- Tests transaction boundaries properly
- Better reflects how the application actually works

### Clarity
- Groups related operations logically
- Separates setup from verification
- Cleaner test structure

## Required Changes

### 1. Group Setup Operations
Combine related setup operations (creating orders and subscriptions) into single transactions:

```scala
// Instead of multiple commits
_ <- store.commit(store.createOrder(order))
_ <- store.commit(store.createSubscription(subscription))

// Use single commit with sequencing
_ <- store.commit(
  store.createOrder(order) *>
  store.createSubscription(subscription)
)
```

### 2. Separate Setup from Queries
Keep setup operations separate from query operations for clarity:

```scala
// Setup phase - single transaction
_ <- store.commit(setupOperations)

// Query phase - separate transactions for each logical query
orders <- store.commit(store.findOrdersByUser(userId))
subscriptions <- store.commit(store.findSubscriptionsByUser(userId))
```

### 3. Use Meaningful Transaction Boundaries
Group operations that should logically be atomic:

```scala
// Good: Related operations together
_ <- store.commit(
  store.createOrder(order) *>
  store.createSubscription(subscription) *>
  store.updateOrder(updatedOrder)
)

// Good: Separate queries for different concerns
orders <- store.commit(store.findOrdersByUser(userId))
activeSubscriptions <- store.commit(store.findActiveSubscriptionsByUser(userId))
```

## Specific Tests to Optimize

### Test: "multiple users and orders should be isolated correctly"
**Current:** 6 separate commits for setup
**Optimized:** 1 commit for all setup operations

### Test: "findActiveSubscriptionsByUser should filter by date and status"
**Current:** 6 separate commits for setup
**Optimized:** 1 commit for all setup operations

### Test: "complete order lifecycle should work"
**Current:** Multiple commits throughout workflow
**Optimized:** Group logical phases into fewer commits

## Implementation Guidelines

### Use `*>` Operator
For operations where you don't need the intermediate results:
```scala
store.createOrder(order1) *>
store.createOrder(order2) *>
store.createSubscription(subscription)
```

### Use `flatMap` for Dependent Operations
When later operations depend on earlier results:
```scala
store.createOrder(order).flatMap { orderId =>
  store.createSubscription(subscription.copy(orderId = orderId))
}
```

### Keep Queries Separate
Don't mix setup and query operations in the same transaction unless necessary:
```scala
// Setup
_ <- store.commit(setupOperations)

// Queries (separate for clarity)
result1 <- store.commit(query1)
result2 <- store.commit(query2)
```

## Acceptance Criteria
- [x] Identify all tests with excessive commits
- [x] Group setup operations into single transactions using `*>`
- [x] Separate setup from verification phases
- [x] Maintain test readability and clarity
- [x] Ensure all tests still pass after optimization
- [x] Verify test performance improvement
- [x] Update helper methods if needed to support batched operations
- [x] `sbt commitCheck` passes with optimized tests

## Expected Impact
- Faster test execution (fewer database round trips)
- More realistic transaction patterns
- Cleaner test structure
- Better separation of concerns between setup and verification

## Status
🟢 **Complete** - All PostgresStore tests optimized with reduced commits