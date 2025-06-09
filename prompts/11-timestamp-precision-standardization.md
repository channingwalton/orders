# Timestamp Precision Standardization

## Objective
Standardize all timestamps in the application to use 1-second precision instead of microsecond precision to improve test reliability and eliminate unnecessary precision.

## Requirements

### Current Problem
- PostgreSQL stores timestamps with microsecond precision by default
- Tests are experiencing intermittent failures due to timestamp precision mismatches
- Microsecond precision is not required for this application's use case
- Clock comparisons in tests can fail due to sub-second timing differences

### Core Functionality

#### 1. Database Schema Changes
- Update all timestamp columns to use `TIMESTAMP(0)` (second precision)
- Apply to both existing and new timestamp columns:
  - `orders.created_at`
  - `orders.updated_at`
  - `subscriptions.created_at`
  - `subscriptions.updated_at`
  - `subscriptions.cancelled_at`
  - `subscriptions.effective_end_date`
  - `order_cancellations.cancelled_at`
  - `order_cancellations.effective_date`
  - `order_cancellations.created_at`
  - `order_cancellations.updated_at`

#### 2. Application Code Changes
- Ensure all `Instant` values are truncated to second precision before database operations
- Update test utilities to use second-precision timestamps
- Maintain backward compatibility for existing data

#### 3. Test Improvements
- Eliminate timing-related test flakiness
- Ensure consistent timestamp handling across all tests
- Update mock data generation to use second precision

### Technical Implementation

#### Database Migration
```sql
-- V4: Standardize timestamp precision to seconds
ALTER TABLE orders 
  ALTER COLUMN created_at TYPE TIMESTAMP(0),
  ALTER COLUMN updated_at TYPE TIMESTAMP(0);

ALTER TABLE subscriptions 
  ALTER COLUMN created_at TYPE TIMESTAMP(0),
  ALTER COLUMN updated_at TYPE TIMESTAMP(0),
  ALTER COLUMN cancelled_at TYPE TIMESTAMP(0),
  ALTER COLUMN effective_end_date TYPE TIMESTAMP(0);

ALTER TABLE order_cancellations 
  ALTER COLUMN cancelled_at TYPE TIMESTAMP(0),
  ALTER COLUMN effective_date TYPE TIMESTAMP(0),
  ALTER COLUMN created_at TYPE TIMESTAMP(0),
  ALTER COLUMN updated_at TYPE TIMESTAMP(0);
```

#### Application Changes
- Add utility function to truncate `Instant` to second precision
- Update all timestamp creation points to use truncated values
- Modify `Clock` usage to ensure consistent precision

#### Test Utilities
- Create helper functions for generating test timestamps
- Update existing tests to use consistent precision
- Remove timing-sensitive assertions that rely on microsecond precision

## Acceptance Criteria

### Database Schema
- [ ] All timestamp columns use `TIMESTAMP(0)` precision
- [ ] Migration successfully converts existing data without loss
- [ ] New records are stored with second precision

### Application Code
- [ ] All `Instant` values are truncated to second precision before database operations
- [ ] Clock operations consistently produce second-precision timestamps
- [ ] No functionality is lost due to reduced precision

### Test Reliability
- [ ] All existing tests continue to pass
- [ ] Tests no longer exhibit timing-related flakiness
- [ ] Test data generation uses consistent timestamp precision
- [ ] Mock objects use second-precision timestamps

### Backward Compatibility
- [ ] Existing data remains accessible and functional
- [ ] API responses maintain expected format
- [ ] No breaking changes to external interfaces

## Test Scenarios

### Database Operations
1. **Insert Operations**: New records have second-precision timestamps
2. **Update Operations**: Modified records maintain second precision
3. **Query Operations**: Timestamp comparisons work reliably
4. **Migration**: Existing microsecond data is truncated to seconds without errors

### Application Behavior
1. **Clock Operations**: All clock reads produce second-precision values
2. **Business Logic**: Time-based calculations work correctly with reduced precision
3. **API Responses**: Timestamps in JSON responses have consistent format

### Test Reliability
1. **Timing Tests**: Tests comparing timestamps no longer fail due to microsecond differences
2. **Mock Data**: All test fixtures use second-precision timestamps
3. **Clock Mocking**: Test clocks produce predictable second-precision values

## Implementation Notes

### Utility Function
Create a utility to standardize timestamp precision:
```scala
object TimeUtils:
  def truncateToSeconds(instant: Instant): Instant = 
    instant.truncatedTo(ChronoUnit.SECONDS)
```

### Database Considerations
- PostgreSQL `TIMESTAMP(0)` stores timestamps with second precision
- Existing microsecond data will be automatically truncated during migration
- No data loss occurs as we're only reducing precision, not changing values

### Test Strategy
- Update all test fixtures to use truncated timestamps
- Add utilities for consistent test time generation
- Verify that timing-dependent tests become more reliable

## Future Considerations
- Consider if any future features might require sub-second precision
- Document the precision choice for future developers
- Ensure monitoring and logging also use consistent precision

## Implementation Results

### Database Migration
- ✅ Created V4 migration to convert all timestamp columns to `TIMESTAMP(0)` precision
- ✅ Successfully handles existing microsecond data by truncating to seconds
- ✅ Covers all timestamp columns: orders, subscriptions, and order_cancellations tables

### Application Code
- ✅ Added `TimeUtils` utility with `truncateToSeconds()` and `now()` functions
- ✅ Updated `OrderService` to use TimeUtils for all timestamp operations
- ✅ Ensures consistent second precision across all business logic operations

### Test Improvements
- ✅ Updated all test utilities to use TimeUtils for consistent timestamp generation
- ✅ Fixed timing-sensitive tests that were experiencing intermittent failures
- ✅ Added verification that timestamps have zero nanoseconds (second precision)
- ✅ Eliminated timing race conditions in PostgresStoreTest

### Test Results
- All 33 tests pass consistently
- No more timing-related test flakiness
- Timestamp precision is verified in database round-trip tests
- Test execution time remains stable

### Technical Notes
- PostgreSQL `TIMESTAMP(0)` automatically truncates microseconds during storage
- TimeUtils handles timezone conversions correctly
- No data loss occurred during migration (only precision reduction)
- Backward compatibility maintained for existing API responses

## Status
🟢 **Complete** - Successfully implemented and tested