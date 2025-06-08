-- Standardize all timestamp columns to use second precision (TIMESTAMP(0))
-- This eliminates microsecond precision to improve test reliability

ALTER TABLE orders 
  ALTER COLUMN created_at TYPE TIMESTAMP(0),
  ALTER COLUMN updated_at TYPE TIMESTAMP(0);

ALTER TABLE subscriptions 
  ALTER COLUMN start_date TYPE TIMESTAMP(0),
  ALTER COLUMN end_date TYPE TIMESTAMP(0),
  ALTER COLUMN cancelled_at TYPE TIMESTAMP(0),
  ALTER COLUMN effective_end_date TYPE TIMESTAMP(0),
  ALTER COLUMN created_at TYPE TIMESTAMP(0),
  ALTER COLUMN updated_at TYPE TIMESTAMP(0);

ALTER TABLE order_cancellations 
  ALTER COLUMN cancelled_at TYPE TIMESTAMP(0),
  ALTER COLUMN effective_date TYPE TIMESTAMP(0),
  ALTER COLUMN created_at TYPE TIMESTAMP(0),
  ALTER COLUMN updated_at TYPE TIMESTAMP(0);