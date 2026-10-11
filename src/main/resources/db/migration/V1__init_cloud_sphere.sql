CREATE SCHEMA IF NOT EXISTS cloud_sphere;

CREATE TABLE cloud_sphere.users (
    id              UUID PRIMARY KEY,
    email           VARCHAR(255) NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    phone           VARCHAR(20),
    status          VARCHAR(32)  NOT NULL,
    loyalty_tier    VARCHAR(32)  NOT NULL,
    failed_attempts INTEGER      NOT NULL DEFAULT 0,
    locked_until    TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL,
    updated_at      TIMESTAMPTZ  NOT NULL,
    CONSTRAINT users_email_unique UNIQUE (email),
    CONSTRAINT users_status_chk CHECK (status IN ('ACTIVE', 'LOCKED', 'DISABLED')),
    CONSTRAINT users_tier_chk CHECK (loyalty_tier IN ('STANDARD', 'GOLD'))
);

CREATE TABLE cloud_sphere.user_roles (
    user_id UUID        NOT NULL REFERENCES cloud_sphere.users (id) ON DELETE CASCADE,
    role    VARCHAR(32) NOT NULL,
    PRIMARY KEY (user_id, role),
    CONSTRAINT user_roles_chk CHECK (role IN ('CUSTOMER', 'ADMIN'))
);

CREATE TABLE cloud_sphere.categories (
    id        UUID PRIMARY KEY,
    name      VARCHAR(120) NOT NULL,
    parent_id UUID REFERENCES cloud_sphere.categories (id),
    CONSTRAINT categories_name_unique UNIQUE (name)
);

CREATE TABLE cloud_sphere.products (
    id          UUID PRIMARY KEY,
    sku         VARCHAR(64)  NOT NULL,
    name        VARCHAR(255) NOT NULL,
    description TEXT,
    price_rwf   BIGINT       NOT NULL,
    stock_qty   INTEGER      NOT NULL,
    status      VARCHAR(32)  NOT NULL,
    category_id UUID REFERENCES cloud_sphere.categories (id),
    version     BIGINT       NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL,
    CONSTRAINT products_sku_unique UNIQUE (sku),
    CONSTRAINT products_price_chk CHECK (price_rwf >= 0),
    CONSTRAINT products_stock_chk CHECK (stock_qty >= 0),
    CONSTRAINT products_status_chk CHECK (status IN ('DRAFT', 'PUBLISHED'))
);

CREATE INDEX idx_products_status_name ON cloud_sphere.products (status, name);

CREATE TABLE cloud_sphere.carts (
    id            UUID PRIMARY KEY,
    user_id       UUID UNIQUE REFERENCES cloud_sphere.users (id),
    guest_token   VARCHAR(64) UNIQUE,
    discount_code VARCHAR(16),
    updated_at    TIMESTAMPTZ NOT NULL,
    CONSTRAINT cart_owner_chk CHECK (
        (user_id IS NOT NULL AND guest_token IS NULL)
        OR (user_id IS NULL AND guest_token IS NOT NULL)
    )
);

CREATE TABLE cloud_sphere.cart_items (
    id             UUID PRIMARY KEY,
    cart_id        UUID    NOT NULL REFERENCES cloud_sphere.carts (id) ON DELETE CASCADE,
    product_id     UUID    NOT NULL REFERENCES cloud_sphere.products (id),
    qty            INTEGER NOT NULL,
    unit_price_rwf BIGINT  NOT NULL,
    CONSTRAINT cart_items_qty_chk CHECK (qty > 0),
    CONSTRAINT cart_items_unique UNIQUE (cart_id, product_id)
);

CREATE TABLE cloud_sphere.discount_codes (
    id           UUID PRIMARY KEY,
    code         VARCHAR(16) NOT NULL,
    percent_off  INTEGER     NOT NULL,
    min_qty      INTEGER     NOT NULL DEFAULT 1,
    active       BOOLEAN     NOT NULL DEFAULT TRUE,
    valid_from   TIMESTAMPTZ,
    valid_until  TIMESTAMPTZ,
    CONSTRAINT discount_codes_unique UNIQUE (code),
    CONSTRAINT discount_codes_pct_chk CHECK (percent_off BETWEEN 0 AND 100),
    CONSTRAINT discount_codes_format_chk CHECK (code ~ '^[A-Z0-9]{5,10}$')
);

CREATE TABLE cloud_sphere.wishlist_items (
    user_id    UUID        NOT NULL REFERENCES cloud_sphere.users (id) ON DELETE CASCADE,
    product_id UUID        NOT NULL REFERENCES cloud_sphere.products (id),
    added_at   TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (user_id, product_id)
);

CREATE TABLE cloud_sphere.orders (
    id               UUID PRIMARY KEY,
    order_number     VARCHAR(32) NOT NULL,
    user_id          UUID        NOT NULL REFERENCES cloud_sphere.users (id),
    status           VARCHAR(32) NOT NULL,
    subtotal_rwf     BIGINT      NOT NULL,
    discount_rwf     BIGINT      NOT NULL,
    shipping_rwf     BIGINT      NOT NULL,
    total_rwf        BIGINT      NOT NULL,
    idempotency_key  VARCHAR(80),
    placed_at        TIMESTAMPTZ NOT NULL,
    updated_at       TIMESTAMPTZ NOT NULL,
    CONSTRAINT orders_number_unique UNIQUE (order_number),
    CONSTRAINT orders_idem_unique UNIQUE (idempotency_key),
    CONSTRAINT orders_status_chk CHECK (status IN (
        'PLACED', 'PAID', 'SHIPPED', 'DELIVERED', 'CANCELLED', 'PAYMENT_TIMED_OUT'
    ))
);

CREATE INDEX idx_orders_user ON cloud_sphere.orders (user_id, placed_at DESC);

CREATE TABLE cloud_sphere.order_lines (
    id             UUID PRIMARY KEY,
    order_id       UUID        NOT NULL REFERENCES cloud_sphere.orders (id) ON DELETE CASCADE,
    product_id     UUID        NOT NULL REFERENCES cloud_sphere.products (id),
    sku            VARCHAR(64) NOT NULL,
    name           VARCHAR(255) NOT NULL,
    qty            INTEGER     NOT NULL,
    unit_price_rwf BIGINT      NOT NULL
);

CREATE TABLE cloud_sphere.payments (
    id          UUID PRIMARY KEY,
    order_id    UUID        NOT NULL UNIQUE REFERENCES cloud_sphere.orders (id),
    provider    VARCHAR(32) NOT NULL,
    amount_rwf  BIGINT      NOT NULL,
    status      VARCHAR(32) NOT NULL,
    gateway_ref VARCHAR(64),
    created_at  TIMESTAMPTZ NOT NULL,
    CONSTRAINT payments_provider_chk CHECK (provider IN ('MTN_MOMO', 'CARD')),
    CONSTRAINT payments_status_chk CHECK (status IN ('PENDING', 'SUCCEEDED', 'DECLINED', 'TIMED_OUT'))
);

CREATE TABLE cloud_sphere.shipments (
    id           UUID PRIMARY KEY,
    order_id     UUID        NOT NULL UNIQUE REFERENCES cloud_sphere.orders (id),
    tracking_no  VARCHAR(64) NOT NULL,
    state        VARCHAR(32) NOT NULL,
    shipped_at   TIMESTAMPTZ NOT NULL,
    delivered_at TIMESTAMPTZ,
    CONSTRAINT shipments_state_chk CHECK (state IN ('SHIPPED', 'DELIVERED'))
);
