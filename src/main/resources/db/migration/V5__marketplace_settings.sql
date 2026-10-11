CREATE TABLE cloud_sphere.marketplace_settings (
 id INTEGER PRIMARY KEY CHECK (id=1), commission_percent INTEGER NOT NULL CHECK (commission_percent BETWEEN 0 AND 50),
 payment_fee_percent INTEGER NOT NULL CHECK (payment_fee_percent BETWEEN 0 AND 50),
 CHECK (commission_percent+payment_fee_percent<=50)
);
INSERT INTO cloud_sphere.marketplace_settings VALUES(1,8,2);
ALTER TABLE cloud_sphere.orders ADD COLUMN commission_percent INTEGER NOT NULL DEFAULT 8;
ALTER TABLE cloud_sphere.orders ADD COLUMN payment_fee_percent INTEGER NOT NULL DEFAULT 2;
-- Existing paid orders already consumed stock in the original application.
UPDATE cloud_sphere.orders o SET stock_reserved=TRUE,escrow_state='HELD'
 FROM cloud_sphere.payments p WHERE p.order_id=o.id AND p.status='SUCCEEDED' AND o.status IN ('PAID','SHIPPED','DELIVERED');
INSERT INTO cloud_sphere.ledger_movements(id,order_id,movement_key,debit_account,credit_account,amount_rwf)
 SELECT o.id,o.id,'capture:'||o.id,'gateway','escrow:'||o.id,o.total_rwf FROM cloud_sphere.orders o
 WHERE o.escrow_state='HELD' AND o.total_rwf>0 ON CONFLICT (movement_key) DO NOTHING;
UPDATE cloud_sphere.orders o SET delivered_at=s.delivered_at FROM cloud_sphere.shipments s WHERE s.order_id=o.id;
UPDATE cloud_sphere.order_lines l SET tracking_no=s.tracking_no,shipped_at=s.shipped_at,delivered_at=s.delivered_at
 FROM cloud_sphere.shipments s WHERE s.order_id=l.order_id;
