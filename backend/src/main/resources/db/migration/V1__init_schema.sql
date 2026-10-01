-- AI Expense Ledger schema.
-- ledger_transactions and transaction_splits are append-only: balances are always derived by
-- summing these rows, and corrections are posted as reversing entries rather than edits.

CREATE TABLE users (
    id          BIGSERIAL PRIMARY KEY,
    email       VARCHAR(255) NOT NULL,
    password    VARCHAR(100) NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE TABLE expense_groups (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(120) NOT NULL,
    created_by  BIGINT       NOT NULL REFERENCES users (id),
    created_at  TIMESTAMPTZ  NOT NULL
);

CREATE TABLE group_members (
    group_id  BIGINT NOT NULL REFERENCES expense_groups (id),
    user_id   BIGINT NOT NULL REFERENCES users (id),
    PRIMARY KEY (group_id, user_id)
);

CREATE INDEX idx_group_members_user ON group_members (user_id);

CREATE TABLE ledger_transactions (
    id                       BIGSERIAL PRIMARY KEY,
    group_id                 BIGINT         NOT NULL REFERENCES expense_groups (id),
    description              VARCHAR(255)   NOT NULL,
    total_amount             NUMERIC(19, 2) NOT NULL,
    payer_id                 BIGINT         NOT NULL REFERENCES users (id),
    transaction_date         DATE           NOT NULL,
    idempotency_key          UUID           NOT NULL,
    reverses_transaction_id  BIGINT         REFERENCES ledger_transactions (id),
    created_by               BIGINT         NOT NULL REFERENCES users (id),
    created_at               TIMESTAMPTZ    NOT NULL,
    -- Rejects duplicate submissions of the same client request, even under concurrent retries.
    CONSTRAINT uk_ledger_idempotency_key UNIQUE (idempotency_key),
    -- A transaction can be reversed at most once.
    CONSTRAINT uk_ledger_reverses_transaction UNIQUE (reverses_transaction_id),
    CONSTRAINT ck_ledger_amount_nonzero CHECK (total_amount <> 0)
);

CREATE INDEX idx_ledger_group_date ON ledger_transactions (group_id, transaction_date DESC, id DESC);

CREATE TABLE transaction_splits (
    id              BIGSERIAL PRIMARY KEY,
    transaction_id  BIGINT         NOT NULL REFERENCES ledger_transactions (id),
    user_id         BIGINT         NOT NULL REFERENCES users (id),
    owed_amount     NUMERIC(19, 2) NOT NULL,
    CONSTRAINT uk_split_transaction_user UNIQUE (transaction_id, user_id)
);

CREATE INDEX idx_splits_transaction ON transaction_splits (transaction_id);

-- Enforce immutability at the database level so no code path (or manual SQL) can rewrite history.
CREATE FUNCTION reject_ledger_mutation() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'Table % is append-only: % is not permitted', TG_TABLE_NAME, TG_OP;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_ledger_transactions_append_only
    BEFORE UPDATE OR DELETE ON ledger_transactions
    FOR EACH ROW EXECUTE FUNCTION reject_ledger_mutation();

CREATE TRIGGER trg_transaction_splits_append_only
    BEFORE UPDATE OR DELETE ON transaction_splits
    FOR EACH ROW EXECUTE FUNCTION reject_ledger_mutation();
