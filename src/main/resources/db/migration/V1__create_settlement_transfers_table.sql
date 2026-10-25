CREATE TABLE settlement_transfers (
                                      id UUID PRIMARY KEY,
                                      idempotency_key VARCHAR(255) NOT NULL,
                                      correlation_id VARCHAR(100) NOT NULL,
                                      source_iban VARCHAR(26) NOT NULL,
                                      destination_iban VARCHAR(26) NOT NULL,
                                      amount BIGINT NOT NULL,
                                      currency VARCHAR(3) NOT NULL DEFAULT 'IRR',
                                      settlement_type VARCHAR(20) NOT NULL,
                                      status VARCHAR(20) NOT NULL,
                                      failure_reason VARCHAR(255),
                                      created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                      settled_at TIMESTAMPTZ,

    -- محدودیت‌های دیتابیس (Constraints) طبق استاندارد پروژه
                                      CONSTRAINT uq_settlement_idempotency_key UNIQUE (idempotency_key),
                                      CONSTRAINT chk_settlement_amount_positive CHECK (amount > 0),
                                      CONSTRAINT chk_settlement_currency CHECK (currency = 'IRR'),
                                      CONSTRAINT chk_settlement_type CHECK (settlement_type IN ('SATNA', 'PAYA')),
                                      CONSTRAINT chk_settlement_status CHECK (status IN ('ACCEPTED', 'PROCESSING', 'SETTLED', 'FAILED'))
);

-- ایندکس‌های مورد نیاز (Indexes) برای سرعت جستجو و گزارش‌گیری روزانه
CREATE INDEX idx_settlement_transfers_status ON settlement_transfers(status);
CREATE INDEX idx_settlement_transfers_created_at ON settlement_transfers(created_at);
CREATE INDEX idx_settlement_transfers_dest_iban ON settlement_transfers(destination_iban);