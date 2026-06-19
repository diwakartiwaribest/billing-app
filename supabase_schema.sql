-- ============================================================
-- Supabase PostgreSQL Schema — Offline-First Billing App
-- ============================================================
-- Run this in Supabase SQL Editor.
-- Every table includes: id (UUID), created_at, updated_at,
-- deleted, version, owner_id for conflict-free offline sync.
-- ============================================================

-- ── Helper: auto-update updated_at + version ────────────────

CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    NEW.version = OLD.version + 1;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- ── Products (formerly shop_items) ──────────────────────────

CREATE TABLE IF NOT EXISTS products (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000',
    name TEXT NOT NULL,
    price NUMERIC NOT NULL DEFAULT 0,
    category TEXT NOT NULL DEFAULT 'General',
    shop_code TEXT NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    version INTEGER NOT NULL DEFAULT 1
);

CREATE INDEX IF NOT EXISTS idx_products_shop ON products(shop_code);
CREATE INDEX IF NOT EXISTS idx_products_updated ON products(updated_at);

DROP TRIGGER IF EXISTS trg_products_updated ON products;
CREATE TRIGGER trg_products_updated
    BEFORE UPDATE ON products
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

ALTER TABLE products ENABLE ROW LEVEL SECURITY;

CREATE POLICY products_read ON products FOR SELECT
    USING (true);

CREATE POLICY products_insert ON products FOR INSERT
    WITH CHECK (true);

CREATE POLICY products_update ON products FOR UPDATE
    USING (true)
    WITH CHECK (true);

-- ── Customers ──────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS customers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id UUID NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000',
    mobile TEXT NOT NULL,
    name TEXT NOT NULL,
    total_bills INTEGER NOT NULL DEFAULT 0,
    total_spent NUMERIC NOT NULL DEFAULT 0,
    pending_amount NUMERIC NOT NULL DEFAULT 0,
    credit_amount NUMERIC NOT NULL DEFAULT 0,
    shop_code TEXT NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    version INTEGER NOT NULL DEFAULT 1
);

CREATE INDEX IF NOT EXISTS idx_customers_mobile ON customers(mobile);
CREATE INDEX IF NOT EXISTS idx_customers_shop ON customers(shop_code);
CREATE INDEX IF NOT EXISTS idx_customers_updated ON customers(updated_at);

DROP TRIGGER IF EXISTS trg_customers_updated ON customers;
CREATE TRIGGER trg_customers_updated
    BEFORE UPDATE ON customers
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

ALTER TABLE customers ENABLE ROW LEVEL SECURITY;

CREATE POLICY customers_read ON customers FOR SELECT USING (true);
CREATE POLICY customers_insert ON customers FOR INSERT WITH CHECK (true);
CREATE POLICY customers_update ON customers FOR UPDATE USING (true) WITH CHECK (true);

-- ── Invoices (formerly bills) ──────────────────────────────

CREATE TABLE IF NOT EXISTS invoices (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000',
    bill_number TEXT NOT NULL DEFAULT '',
    customer_name TEXT NOT NULL DEFAULT '',
    customer_mobile TEXT NOT NULL DEFAULT '',
    total_amount NUMERIC NOT NULL DEFAULT 0,
    payment_status TEXT NOT NULL DEFAULT 'paid',
    created_by TEXT NOT NULL DEFAULT '',
    shop_code TEXT NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    version INTEGER NOT NULL DEFAULT 1
);

CREATE INDEX IF NOT EXISTS idx_invoices_shop ON invoices(shop_code);
CREATE INDEX IF NOT EXISTS idx_invoices_mobile ON invoices(customer_mobile);
CREATE INDEX IF NOT EXISTS idx_invoices_updated ON invoices(updated_at);

DROP TRIGGER IF EXISTS trg_invoices_updated ON invoices;
CREATE TRIGGER trg_invoices_updated
    BEFORE UPDATE ON invoices
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

ALTER TABLE invoices ENABLE ROW LEVEL SECURITY;

CREATE POLICY invoices_read ON invoices FOR SELECT USING (true);
CREATE POLICY invoices_insert ON invoices FOR INSERT WITH CHECK (true);
CREATE POLICY invoices_update ON invoices FOR UPDATE USING (true) WITH CHECK (true);

-- ── Invoice Items (formerly bill_items) ─────────────────────

CREATE TABLE IF NOT EXISTS invoice_items (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000',
    invoice_id UUID NOT NULL,
    item_name TEXT NOT NULL,
    quantity INTEGER NOT NULL DEFAULT 1,
    unit_price NUMERIC NOT NULL DEFAULT 0,
    subtotal NUMERIC NOT NULL DEFAULT 0,
    shop_code TEXT NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    version INTEGER NOT NULL DEFAULT 1
);

CREATE INDEX IF NOT EXISTS idx_invoice_items_invoice ON invoice_items(invoice_id);
CREATE INDEX IF NOT EXISTS idx_invoice_items_shop ON invoice_items(shop_code);
CREATE INDEX IF NOT EXISTS idx_invoice_items_updated ON invoice_items(updated_at);

DROP TRIGGER IF EXISTS trg_invoice_items_updated ON invoice_items;
CREATE TRIGGER trg_invoice_items_updated
    BEFORE UPDATE ON invoice_items
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

ALTER TABLE invoice_items ENABLE ROW LEVEL SECURITY;

CREATE POLICY invoice_items_read ON invoice_items FOR SELECT USING (true);
CREATE POLICY invoice_items_insert ON invoice_items FOR INSERT WITH CHECK (true);
CREATE POLICY invoice_items_update ON invoice_items FOR UPDATE USING (true) WITH CHECK (true);

-- ── Customer Payments ──────────────────────────────────────

CREATE TABLE IF NOT EXISTS customer_payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id UUID NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000',
    customer_mobile TEXT NOT NULL,
    amount NUMERIC NOT NULL DEFAULT 0,
    note TEXT NOT NULL DEFAULT '',
    shop_code TEXT NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    version INTEGER NOT NULL DEFAULT 1
);

CREATE INDEX IF NOT EXISTS idx_cpayments_mobile ON customer_payments(customer_mobile);
CREATE INDEX IF NOT EXISTS idx_cpayments_shop ON customer_payments(shop_code);
CREATE INDEX IF NOT EXISTS idx_cpayments_updated ON customer_payments(updated_at);

DROP TRIGGER IF EXISTS trg_customer_payments_updated ON customer_payments;
CREATE TRIGGER trg_customer_payments_updated
    BEFORE UPDATE ON customer_payments
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

ALTER TABLE customer_payments ENABLE ROW LEVEL SECURITY;

CREATE POLICY cpayments_read ON customer_payments FOR SELECT USING (true);
CREATE POLICY cpayments_insert ON customer_payments FOR INSERT WITH CHECK (true);
CREATE POLICY cpayments_update ON customer_payments FOR UPDATE USING (true) WITH CHECK (true);

-- ── Enable Realtime for all sync tables ─────────────────────

ALTER PUBLICATION supabase_realtime ADD TABLE products;
ALTER PUBLICATION supabase_realtime ADD TABLE customers;
ALTER PUBLICATION supabase_realtime ADD TABLE invoices;
ALTER PUBLICATION supabase_realtime ADD TABLE invoice_items;
ALTER PUBLICATION supabase_realtime ADD TABLE customer_payments;
