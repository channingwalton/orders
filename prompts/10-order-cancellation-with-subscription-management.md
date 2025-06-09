# Order Cancellation with Subscription Management

## Objective
Implement comprehensive order cancellation that properly manages associated subscriptions and provides cancellation tracking.

## Requirements

### Current Gap
Currently when an order is cancelled, the associated subscription remains active. This creates an inconsistent state where users have cancelled orders but still have active subscriptions.

### Core Functionality

#### 1. Enhanced Order Cancellation
- When an order is cancelled, automatically cancel the associated subscription
- Add cancellation reason tracking
- Add cancellation timestamp
- Ensure atomic operation (both order and subscription cancelled together)

#### 2. Subscription Cancellation Logic
- Update subscription status to "cancelled" 
- Calculate cancellation date (immediate vs end of current period)
- Preserve subscription data for historical tracking
- Handle edge cases (already expired, already cancelled)

#### 3. Cancellation Policies
- **Immediate Cancellation**: Subscription ends immediately
- **End of Period Cancellation**: Subscription remains active until natural expiration
- **Grace Period**: Allow brief period for reactivation

### API Changes

#### Enhanced Cancel Order Endpoint
```
PUT /orders/{orderId}/cancel
Content-Type: application/json

{
  "reason": "string",                    // Optional: user_request, payment_failure, violation, etc.
  "cancellationType": "immediate|end_of_period",  // Default: immediate
  "notes": "string"                      // Optional: additional context
}
```

#### New Endpoints
```
GET /orders/{orderId}/cancellation      // Get cancellation details
POST /subscriptions/{subscriptionId}/reactivate  // Reactivate within grace period
```

### Data Model Changes

#### Order Cancellation Details
```json
{
  "id": "uuid",
  "orderId": "uuid", 
  "reason": "user_request|payment_failure|violation|other",
  "cancellationType": "immediate|end_of_period",
  "notes": "string",
  "cancelledAt": "2024-01-01T00:00:00Z",
  "cancelledBy": "user|system|admin",
  "effectiveDate": "2024-01-01T00:00:00Z"  // When cancellation takes effect
}
```

#### Updated Subscription Status
- Add `cancelled` status alongside existing `active`, `expired`
- Add `cancelledAt` timestamp field
- Add `effectiveEndDate` for end-of-period cancellations

### Business Rules

#### Cancellation Timing
1. **Immediate**: Subscription ends at cancellation time
2. **End of Period**: Subscription ends at natural expiration date
3. **Grace Period**: 24-48 hours to reactivate immediate cancellations

#### Refund Logic (Future Enhancement)
- Calculate prorated refund based on unused time
- Different policies for monthly vs annual subscriptions
- No refunds for subscriptions used > 80% of period

### Technical Implementation

#### Database Changes
```sql
-- New cancellation tracking table
CREATE TABLE order_cancellations (
  id UUID PRIMARY KEY,
  order_id UUID NOT NULL REFERENCES orders(id),
  reason VARCHAR(50) NOT NULL,
  cancellation_type VARCHAR(20) NOT NULL,
  notes TEXT,
  cancelled_at TIMESTAMP NOT NULL,
  cancelled_by VARCHAR(20) NOT NULL,
  effective_date TIMESTAMP NOT NULL,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL
);

-- Update subscriptions table
ALTER TABLE subscriptions 
ADD COLUMN cancelled_at TIMESTAMP,
ADD COLUMN effective_end_date TIMESTAMP;
```

#### Service Layer Changes
- Update `OrderService.cancelOrder()` to handle subscription cancellation
- Add `SubscriptionService` with cancellation methods
- Implement transaction boundaries to ensure atomicity
- Add validation for cancellation business rules

#### Store Layer Changes
- Add `findSubscriptionsByOrder()` method
- Add `cancelSubscription()` method
- Add `createOrderCancellation()` method
- Update `findActiveSubscriptionsByUser()` to exclude cancelled

## Acceptance Criteria

### Core Functionality
- [ ] Cancelling an order automatically cancels associated subscription
- [ ] Cancellation reason and type are captured and stored
- [ ] Both immediate and end-of-period cancellation types work correctly
- [ ] Atomic operation ensures both order and subscription are cancelled together
- [ ] Cancelled subscriptions no longer appear in active subscription queries

### API Endpoints
- [ ] Enhanced `PUT /orders/{orderId}/cancel` accepts cancellation details
- [ ] `GET /orders/{orderId}/cancellation` returns cancellation information
- [ ] Proper error handling for invalid cancellation requests
- [ ] Cannot cancel already cancelled orders
- [ ] Cannot cancel orders with already expired subscriptions

### Data Integrity
- [ ] All cancellation data is properly persisted
- [ ] Subscription status correctly reflects cancellation
- [ ] Historical subscription data is preserved
- [ ] Database constraints prevent invalid states

### Business Logic
- [ ] Immediate cancellation sets subscription end date to cancellation time
- [ ] End-of-period cancellation preserves original end date
- [ ] User subscription status API reflects cancellation correctly
- [ ] Grace period logic works for reactivation scenarios

### Testing
- [ ] Unit tests for all cancellation scenarios
- [ ] Integration tests for API endpoints
- [ ] Database tests for new queries and constraints
- [ ] Edge case testing (already cancelled, expired, etc.)

## Test Scenarios

### Happy Path
1. **Immediate Cancellation**: Cancel active order → subscription immediately cancelled
2. **End of Period**: Cancel active order → subscription remains active until expiration
3. **With Reason**: Cancel with specific reason → reason properly stored and retrievable

### Edge Cases
1. **Already Cancelled Order**: Attempt to cancel cancelled order → proper error
2. **Expired Subscription**: Cancel order with expired subscription → handle gracefully
3. **Multiple Subscriptions**: Cancel order with multiple subscriptions → all cancelled
4. **Concurrent Cancellation**: Multiple cancellation requests → handle race conditions

### Integration Scenarios
1. **Full Lifecycle**: Create order → use subscription → cancel → verify all states
2. **User Journey**: User checks status → cancels → checks status again → sees cancelled state
3. **Admin Operations**: System-initiated cancellation → proper audit trail

## Sample Usage

```bash
# Cancel order with reason
curl -X PUT http://localhost:8080/orders/abc-123/cancel \
  -H "Content-Type: application/json" \
  -d '{
    "reason": "user_request",
    "cancellationType": "immediate", 
    "notes": "Customer requested cancellation due to billing issue"
  }'

# Get cancellation details  
curl http://localhost:8080/orders/abc-123/cancellation

# Check subscription status (should show cancelled)
curl http://localhost:8080/users/user123/subscription-status
```

## Dependencies
- Requires existing order and subscription management
- May need database migration for new cancellation table
- Consider impact on existing subscription status queries

## Future Enhancements
- Refund calculation and processing
- Cancellation analytics and reporting
- Automated cancellation for payment failures
- Customer communication integration
- Cancellation survey collection

## Status
⚪ **Pending** - Not started yet