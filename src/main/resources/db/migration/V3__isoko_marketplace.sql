-- Preserve the original schema and migration history for existing installations.
ALTER TABLE cloud_sphere.user_roles DROP CONSTRAINT user_roles_chk;
ALTER TABLE cloud_sphere.user_roles ADD CONSTRAINT user_roles_chk CHECK (role IN ('CUSTOMER','SELLER','ADMIN'));
CREATE TABLE cloud_sphere.shops (
 id UUID PRIMARY KEY, owner_id UUID NOT NULL UNIQUE REFERENCES cloud_sphere.users(id),
 name VARCHAR(120) NOT NULL, description TEXT, business_reference VARCHAR(120) NOT NULL,
 status VARCHAR(32) NOT NULL CHECK (status IN ('PENDING','APPROVED','REJECTED','SUSPENDED')),
 review_note TEXT, created_at TIMESTAMPTZ NOT NULL
);
ALTER TABLE cloud_sphere.products ADD COLUMN shop_id UUID REFERENCES cloud_sphere.shops(id);
CREATE INDEX idx_products_shop ON cloud_sphere.products(shop_id);
ALTER TABLE cloud_sphere.order_lines ADD COLUMN shop_id UUID REFERENCES cloud_sphere.shops(id);
ALTER TABLE cloud_sphere.order_lines ADD COLUMN shop_name VARCHAR(120);
ALTER TABLE cloud_sphere.order_lines ADD COLUMN tracking_no VARCHAR(64);
ALTER TABLE cloud_sphere.order_lines ADD COLUMN delivery_otp_hash VARCHAR(255);
ALTER TABLE cloud_sphere.order_lines ADD COLUMN shipped_at TIMESTAMPTZ;
ALTER TABLE cloud_sphere.order_lines ADD COLUMN delivered_at TIMESTAMPTZ;
ALTER TABLE cloud_sphere.orders ADD COLUMN escrow_state VARCHAR(32) NOT NULL DEFAULT 'NONE';
ALTER TABLE cloud_sphere.orders ADD COLUMN delivery_method VARCHAR(32) NOT NULL DEFAULT 'HOME_KIGALI';
ALTER TABLE cloud_sphere.orders ADD COLUMN delivery_address VARCHAR(255);
ALTER TABLE cloud_sphere.orders ADD COLUMN delivered_at TIMESTAMPTZ;
ALTER TABLE cloud_sphere.orders ADD COLUMN dispute_reason TEXT;
ALTER TABLE cloud_sphere.orders ADD COLUMN dispute_evidence VARCHAR(500);
ALTER TABLE cloud_sphere.orders ADD COLUMN resolution_note TEXT;
ALTER TABLE cloud_sphere.orders ADD COLUMN stock_reserved BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE cloud_sphere.orders ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE cloud_sphere.orders DROP CONSTRAINT orders_status_chk;
ALTER TABLE cloud_sphere.orders ADD CONSTRAINT orders_status_chk CHECK (status IN
 ('PLACED','PAYMENT_PENDING','PAID','SHIPPED','DELIVERED','COMPLETED','DISPUTED','REFUNDED','CANCELLED','PAYMENT_TIMED_OUT'));
ALTER TABLE cloud_sphere.payments DROP CONSTRAINT payments_provider_chk;
ALTER TABLE cloud_sphere.payments ADD CONSTRAINT payments_provider_chk CHECK (provider IN ('MTN_MOMO','AIRTEL_MONEY','CARD'));
CREATE TABLE cloud_sphere.ledger_movements (
 id UUID PRIMARY KEY, order_id UUID NOT NULL REFERENCES cloud_sphere.orders(id),
 movement_key VARCHAR(160) NOT NULL UNIQUE, debit_account VARCHAR(160) NOT NULL,
 credit_account VARCHAR(160) NOT NULL, amount_rwf BIGINT NOT NULL CHECK (amount_rwf > 0),
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(), CHECK (debit_account <> credit_account)
);
CREATE TABLE cloud_sphere.notifications (
 id UUID PRIMARY KEY, user_id UUID NOT NULL REFERENCES cloud_sphere.users(id),
 message TEXT NOT NULL, created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TABLE cloud_sphere.audit_events (
 id UUID PRIMARY KEY, actor_id UUID REFERENCES cloud_sphere.users(id),
 action VARCHAR(80) NOT NULL, subject_id UUID NOT NULL, details TEXT,
 created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE FUNCTION cloud_sphere.reject_history_change() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN RAISE EXCEPTION 'History is append-only'; END; $$;
CREATE TRIGGER ledger_append_only BEFORE UPDATE OR DELETE ON cloud_sphere.ledger_movements
 FOR EACH ROW EXECUTE FUNCTION cloud_sphere.reject_history_change();
CREATE TRIGGER audit_append_only BEFORE UPDATE OR DELETE ON cloud_sphere.audit_events
 FOR EACH ROW EXECUTE FUNCTION cloud_sphere.reject_history_change();
