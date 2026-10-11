ALTER TABLE cloud_sphere.users ADD COLUMN phone_verified BOOLEAN NOT NULL DEFAULT FALSE;
CREATE UNIQUE INDEX users_verified_phone_unique ON cloud_sphere.users(phone) WHERE phone_verified;
CREATE TABLE cloud_sphere.auth_challenges (
 id UUID PRIMARY KEY, purpose VARCHAR(32) NOT NULL, user_id UUID REFERENCES cloud_sphere.users(id),
 email VARCHAR(255), phone VARCHAR(20), password_hash VARCHAR(255), code_hash VARCHAR(255) NOT NULL,
 guest_token VARCHAR(64), expires_at TIMESTAMPTZ NOT NULL, attempts INTEGER NOT NULL DEFAULT 0,
 consumed BOOLEAN NOT NULL DEFAULT FALSE, created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_challenges_phone_time ON cloud_sphere.auth_challenges(phone,created_at);
