-- Seed data for core-service — matches Phase 1 mock data

-- Accounts for default test customer (custIdentNum = 123456789)
INSERT INTO accounts (account_number, account_type, balance, customer_id)
VALUES ('ACC-001', 'SAVINGS', 1000.00, '123456789')
ON CONFLICT (account_number) DO NOTHING;

INSERT INTO accounts (account_number, account_type, balance, customer_id)
VALUES ('ACC-002', 'CHECKING', 2500.50, '123456789')
ON CONFLICT (account_number) DO NOTHING;

-- Cards
INSERT INTO cards (card_number, card_type, credit_limit, available_balance, customer_id)
VALUES ('CARD-001', 'CREDIT', 5000.00, 3200.00, '123456789')
ON CONFLICT (card_number) DO NOTHING;

INSERT INTO cards (card_number, card_type, credit_limit, available_balance, customer_id)
VALUES ('CARD-002', 'DEBIT', NULL, 1000.00, '123456789')
ON CONFLICT (card_number) DO NOTHING;

-- Balance summary
INSERT INTO balances (customer_id, total_balance, available_balance)
VALUES ('123456789', 3500.50, 3200.00)
ON CONFLICT (customer_id) DO NOTHING;
