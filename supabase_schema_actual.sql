-- ============================================================
-- Supabase PostgreSQL Schema — Actual Deployed Table Names
-- Run this in Supabase SQL Editor to create/reconcile tables.
-- ============================================================

-- Helper function for updated_at + version bumping
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    IF OLD.version IS NOT NULL THEN
        NEW.version = OLD.version + 1;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 1. shops — config & credentials per shop
CREATE TABLE IF NOT EXISTS shops (
    code TEXT PRIMARY KEY,
    secret TEXT NOT NULL DEFAULT '',
    supabase_url TEXT DEFAULT '',
    supabase_key TEXT DEFAULT '',
    project_ref TEXT DEFAULT '',
    pat TEXT DEFAULT '',
    shop_name TEXT DEFAULT '',
    sync_enabled BOOLEAN DEFAULT false,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 2. bills — invoices (bill-of-sale records)
CREATE TABLE IF NOT EXISTS bills (
    id TEXT PRIMARY KEY,
    shop_code TEXT NOT NULL,
    bill_number TEXT DEFAULT '',
    customer_name TEXT DEFAULT '',
    customer_mobile TEXT DEFAULT '',
    total_amount REAL NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    created_by TEXT DEFAULT '',
    payment_status TEXT DEFAULT 'paid',
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    version INTEGER NOT NULL DEFAULT 1,
    owner_id TEXT DEFAULT ''
);
DROP TRIGGER IF EXISTS trg_bills_updated ON bills;
CREATE TRIGGER trg_bills_updated
    BEFORE UPDATE ON bills
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- 3. bill_items — line items within a bill
CREATE TABLE IF NOT EXISTS bill_items (
    id TEXT PRIMARY KEY,
    bill_id TEXT NOT NULL,
    shop_code TEXT NOT NULL,
    item_name TEXT NOT NULL,
    quantity INTEGER NOT NULL DEFAULT 1,
    unit_price REAL NOT NULL DEFAULT 0,
    subtotal REAL NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    version INTEGER NOT NULL DEFAULT 1,
    owner_id TEXT DEFAULT ''
);
DROP TRIGGER IF EXISTS trg_bill_items_updated ON bill_items;
CREATE TRIGGER trg_bill_items_updated
    BEFORE UPDATE ON bill_items
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- 4. shop_items — products/inventory
CREATE TABLE IF NOT EXISTS shop_items (
    id TEXT PRIMARY KEY,
    shop_code TEXT NOT NULL,
    name TEXT NOT NULL,
    price REAL NOT NULL DEFAULT 0,
    category TEXT DEFAULT 'General',
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    version INTEGER NOT NULL DEFAULT 1,
    owner_id TEXT DEFAULT ''
);
DROP TRIGGER IF EXISTS trg_shop_items_updated ON shop_items;
CREATE TRIGGER trg_shop_items_updated
    BEFORE UPDATE ON shop_items
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- 5. customers
CREATE TABLE IF NOT EXISTS customers (
    mobile TEXT NOT NULL,
    shop_code TEXT NOT NULL,
    name TEXT DEFAULT '',
    total_bills INTEGER DEFAULT 0,
    total_spent REAL DEFAULT 0,
    pending_amount REAL DEFAULT 0,
    credit_amount REAL DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    version INTEGER NOT NULL DEFAULT 1,
    owner_id TEXT DEFAULT '',
    PRIMARY KEY (mobile, shop_code)
);
DROP TRIGGER IF EXISTS trg_customers_updated ON customers;
CREATE TRIGGER trg_customers_updated
    BEFORE UPDATE ON customers
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- 6. customer_payments — ledger entries
CREATE TABLE IF NOT EXISTS customer_payments (
    uuid TEXT PRIMARY KEY,
    shop_code TEXT NOT NULL,
    customer_mobile TEXT NOT NULL,
    amount REAL NOT NULL DEFAULT 0,
    note TEXT DEFAULT '',
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    version INTEGER NOT NULL DEFAULT 1,
    owner_id TEXT DEFAULT ''
);
DROP TRIGGER IF EXISTS trg_customer_payments_updated ON customer_payments;
CREATE TRIGGER trg_customer_payments_updated
    BEFORE UPDATE ON customer_payments
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- 7. shop_settings — per-shop display config
CREATE TABLE IF NOT EXISTS shop_settings (
    shop_code TEXT PRIMARY KEY,
    shop_name TEXT DEFAULT '',
    shop_address TEXT DEFAULT '',
    shop_phone TEXT DEFAULT '',
    shop_logo TEXT DEFAULT '',
    invoice_message TEXT DEFAULT '',
    categories TEXT DEFAULT ''
);

-- 8. user_shops — role mapping
CREATE TABLE IF NOT EXISTS user_shops (
    user_id TEXT NOT NULL,
    shop_code TEXT NOT NULL,
    role TEXT DEFAULT 'member',
    device_name TEXT DEFAULT '',
    email TEXT DEFAULT '',
    created_at TIMESTAMPTZ DEFAULT NOW(),
    PRIMARY KEY (user_id, shop_code)
);

-- Indexes for delta sync performance
CREATE INDEX IF NOT EXISTS idx_bills_updated ON bills(updated_at);
CREATE INDEX IF NOT EXISTS idx_bill_items_updated ON bill_items(updated_at);
CREATE INDEX IF NOT EXISTS idx_shop_items_updated ON shop_items(updated_at);
CREATE INDEX IF NOT EXISTS idx_customers_updated ON customers(updated_at);
CREATE INDEX IF NOT EXISTS idx_customer_payments_updated ON customer_payments(updated_at);
CREATE INDEX IF NOT EXISTS idx_bills_shop ON bills(shop_code);
CREATE INDEX IF NOT EXISTS idx_bill_items_shop ON bill_items(shop_code);
CREATE INDEX IF NOT EXISTS idx_customers_shop ON customers(shop_code);
CREATE INDEX IF NOT EXISTS idx_customer_payments_shop ON customer_payments(shop_code);

-- Enable RLS (open policies for simplicity; tighten in production)
ALTER TABLE shops ENABLE ROW LEVEL SECURITY;
ALTER TABLE bills ENABLE ROW LEVEL SECURITY;
ALTER TABLE bill_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE shop_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE customers ENABLE ROW LEVEL SECURITY;
ALTER TABLE customer_payments ENABLE ROW LEVEL SECURITY;
ALTER TABLE shop_settings ENABLE ROW LEVEL SECURITY;
ALTER TABLE user_shops ENABLE ROW LEVEL SECURITY;

DO $$ BEGIN
    CREATE POLICY "Allow all" ON shops FOR ALL USING (true) WITH CHECK (true);
    CREATE POLICY "Allow all" ON bills FOR ALL USING (true) WITH CHECK (true);
    CREATE POLICY "Allow all" ON bill_items FOR ALL USING (true) WITH CHECK (true);
    CREATE POLICY "Allow all" ON shop_items FOR ALL USING (true) WITH CHECK (true);
    CREATE POLICY "Allow all" ON customers FOR ALL USING (true) WITH CHECK (true);
    CREATE POLICY "Allow all" ON customer_payments FOR ALL USING (true) WITH CHECK (true);
    CREATE POLICY "Allow all" ON shop_settings FOR ALL USING (true) WITH CHECK (true);
    CREATE POLICY "Allow all" ON user_shops FOR ALL USING (true) WITH CHECK (true);
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

-- Enable Realtime publication
ALTER PUBLICATION supabase_realtime ADD TABLE IF NOT EXISTS
    bills, bill_items, shop_items, customers, customer_payments, shop_settings, user_shops;
