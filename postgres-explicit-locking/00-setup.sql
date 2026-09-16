-- Shared schema for every demo in this folder. Run it before each scenario.
--
-- These are the SQL scenarios behind the article "Explicit Locking in PostgreSQL:
-- The Deadlock You Did Not Ask For". Each file is numbered step by step so you can
-- run it by hand in two or three psql sessions and watch the blocking happen.
--
-- Environment used for the outputs quoted in the article:
--   PostgreSQL 18.6 in Docker, autovacuum off, port 5433
--   autovacuum is off so nothing is cleaned up in the background while you watch
--
--   docker run -d --name b2-locks -p 5433:5432 \
--     -e POSTGRES_PASSWORD=postgres -e POSTGRES_DB=b2 \
--     postgres:18.6 -c autovacuum=off
--
--   psql -h localhost -p 5433 -U postgres -d b2 -f 00-setup.sql
--
-- What each file shows:
--   D01   two ordinary UPDATEs deadlock; no explicit lock anywhere -> 40P01
--   D01b  the victim is chosen by deadlock_timeout, per session
--   D02   ORDER BY id FOR UPDATE: the waiter waits holding nothing
--   D03   ACCESS EXCLUSIVE is the only mode that blocks a plain SELECT
--   D03b  CREATE INDEX stops writes; CONCURRENTLY does not
--   D04   FK checks take FOR KEY SHARE, so name updates do not wait
--   D05   a row locked FOR UPDATE is still readable
--   D06   FOR UPDATE at Read Committed / Repeatable Read / Serializable
--   D07   two FOR SHARE holders upgrading to FOR UPDATE deadlock
--   D08   pg_try_advisory_lock: one winner, the others skip
--   D09   a session-level advisory lock survives ROLLBACK
--   D10   reading pg_locks and pg_stat_activity to find the blocker

DROP TABLE IF EXISTS orders, customers, accounts CASCADE;

CREATE TABLE accounts (
    id      int PRIMARY KEY,
    owner   text NOT NULL,
    balance numeric(12,2) NOT NULL
);
INSERT INTO accounts VALUES (100, 'Ali', 1000.00), (200, 'Ayse', 1000.00);

-- for the foreign key demo (D04)
CREATE TABLE customers (
    id   int PRIMARY KEY,
    name text NOT NULL
);
INSERT INTO customers VALUES (42, 'Ali');

CREATE TABLE orders (
    id          serial PRIMARY KEY,
    customer_id int REFERENCES customers(id),
    amount      numeric(12,2)
);
