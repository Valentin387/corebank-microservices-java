-- Schema for core-service (Product/Home bounded context)

CREATE TABLE IF NOT EXISTS accounts (
    id BIGSERIAL PRIMARY KEY,
    account_number VARCHAR(20) NOT NULL UNIQUE,
    account_type VARCHAR(20) NOT NULL,
    balance NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    customer_id VARCHAR(50) NOT NULL
);

CREATE TABLE IF NOT EXISTS cards (
    id BIGSERIAL PRIMARY KEY,
    card_number VARCHAR(20) NOT NULL UNIQUE,
    card_type VARCHAR(20) NOT NULL,
    credit_limit NUMERIC(15, 2),
    available_balance NUMERIC(15, 2),
    customer_id VARCHAR(50) NOT NULL
);

CREATE TABLE IF NOT EXISTS balances (
    id BIGSERIAL PRIMARY KEY,
    customer_id VARCHAR(50) NOT NULL UNIQUE,
    total_balance NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    available_balance NUMERIC(15, 2) NOT NULL DEFAULT 0.00
);

CREATE INDEX IF NOT EXISTS idx_accounts_customer_id ON accounts(customer_id);
CREATE INDEX IF NOT EXISTS idx_cards_customer_id ON cards(customer_id);
CREATE INDEX IF NOT EXISTS idx_balances_customer_id ON balances(customer_id);
