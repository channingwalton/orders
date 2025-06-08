CREATE TABLE order_cancellations (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    reason VARCHAR(50) NOT NULL,
    cancellation_type VARCHAR(20) NOT NULL,
    notes TEXT,
    cancelled_at TIMESTAMPTZ NOT NULL,
    cancelled_by VARCHAR(20) NOT NULL,
    effective_date TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

ALTER TABLE subscriptions 
ADD COLUMN cancelled_at TIMESTAMPTZ,
ADD COLUMN effective_end_date TIMESTAMPTZ;

CREATE INDEX idx_order_cancellations_order_id ON order_cancellations(order_id);
CREATE INDEX idx_order_cancellations_cancelled_at ON order_cancellations(cancelled_at);
CREATE INDEX idx_subscriptions_cancelled_at ON subscriptions(cancelled_at);