-- OP-009: grants and schemes (spec p.5-6), plus the stage and sector lookup tables they point to.

-- ---------------------------------------------------------------------------------------------
-- Lookup tables. OP-025 makes these admin-editable. The seed values below are placeholders:
-- T-Hub sets the real dropdown values.
-- ---------------------------------------------------------------------------------------------
CREATE TABLE stage (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name       VARCHAR(100) NOT NULL UNIQUE,
    sort_order INT          NOT NULL DEFAULT 0,
    active     BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE TABLE sector (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name       VARCHAR(100) NOT NULL UNIQUE,
    sort_order INT          NOT NULL DEFAULT 0,
    active     BOOLEAN      NOT NULL DEFAULT TRUE
);

INSERT INTO stage (name, sort_order) VALUES
    ('Idea', 1),
    ('Prototype / MVP', 2),
    ('Early revenue', 3),
    ('Growth', 4),
    ('Scale', 5);

INSERT INTO sector (name, sort_order) VALUES
    ('AgriTech', 1),
    ('CleanTech', 2),
    ('DeepTech', 3),
    ('EdTech', 4),
    ('FinTech', 5),
    ('HealthTech', 6),
    ('SaaS', 7),
    ('Other', 8);

-- ---------------------------------------------------------------------------------------------
-- Grants. "grant" is a reserved word in PostgreSQL, so it must always be double-quoted.
-- Enums are stored as VARCHAR (the JPA side uses @Enumerated(EnumType.STRING)), never as numbers.
-- A CHECK constraint keeps bad values out even if something bypasses the application.
-- ---------------------------------------------------------------------------------------------
CREATE TABLE "grant" (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name               VARCHAR(255)  NOT NULL,
    issuing_body       VARCHAR(255)  NOT NULL,
    issuing_body_type  VARCHAR(20)   NOT NULL,
    type               VARCHAR(20)   NOT NULL,
    amount_min         NUMERIC(15, 2),
    amount_max         NUMERIC(15, 2),
    amount_display     VARCHAR(255),
    deadline           DATE,                       -- NULL means a rolling grant (no deadline)
    summary            TEXT          NOT NULL,
    eligibility        TEXT,
    what_is_funded     TEXT,
    how_to_apply       TEXT,
    source_url         VARCHAR(2048) NOT NULL,
    published          BOOLEAN       NOT NULL DEFAULT FALSE,
    expired            BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at         TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ   NOT NULL DEFAULT now(),

    CONSTRAINT grant_issuing_body_type_check CHECK (issuing_body_type IN ('CENTRAL', 'STATE', 'OTHER')),
    CONSTRAINT grant_type_check CHECK (type IN ('GRANT', 'SUBSIDY', 'SCHEME', 'INCENTIVE')),
    CONSTRAINT grant_amount_range_check CHECK (amount_min IS NULL OR amount_max IS NULL OR amount_min <= amount_max)
);

-- Indexes for the founder listing filters (OP-011..OP-013).
CREATE INDEX idx_grant_published_expired ON "grant" (published, expired);
CREATE INDEX idx_grant_deadline          ON "grant" (deadline);
CREATE INDEX idx_grant_issuing_body_type ON "grant" (issuing_body_type);
CREATE INDEX idx_grant_type              ON "grant" (type);

-- ---------------------------------------------------------------------------------------------
-- Many-to-many: which stages and sectors a grant is open to.
-- The primary key (grant_id, x_id) already serves lookups by grant; the extra index serves
-- "all grants for this stage/sector" (the listing filters).
-- ---------------------------------------------------------------------------------------------
CREATE TABLE grant_stage (
    grant_id UUID NOT NULL REFERENCES "grant" (id) ON DELETE CASCADE,
    stage_id UUID NOT NULL REFERENCES stage (id),
    PRIMARY KEY (grant_id, stage_id)
);
CREATE INDEX idx_grant_stage_stage_id ON grant_stage (stage_id);

CREATE TABLE grant_sector (
    grant_id  UUID NOT NULL REFERENCES "grant" (id) ON DELETE CASCADE,
    sector_id UUID NOT NULL REFERENCES sector (id),
    PRIMARY KEY (grant_id, sector_id)
);
CREATE INDEX idx_grant_sector_sector_id ON grant_sector (sector_id);
