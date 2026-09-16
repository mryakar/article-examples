-- D04 - why the middle modes exist: FK checks must not block ordinary updates.  Section 4
-- THREE terminals.  First: psql -d b2 -f 00-setup.sql

-- ==================== T1 ====================
-- step 1
BEGIN;
-- step 2 - the FK check takes FOR KEY SHARE on customers(42)
INSERT INTO orders(customer_id, amount) VALUES (42, 10);
-- step 5
ROLLBACK;

-- ==================== T2 ====================
-- step 3 - non-key column -> FOR NO KEY UPDATE -> no conflict -> DOES NOT WAIT
BEGIN;
UPDATE customers SET name = 'New Name' WHERE id = 42;
ROLLBACK;      -- IMPORTANT: finish before T3, or T3 waits on T2 as well (see the note)

-- ==================== T3 ====================
-- step 4 - DELETE always takes FOR UPDATE -> conflicts -> WAITS
BEGIN;
DELETE FROM customers WHERE id = 42;
-- released when T1 rolls back
ROLLBACK;

-- Observed: T2 did not wait, T3 did, and T3 was released when T1 rolled back.
--
-- How Postgres picks the mode:
--   UPDATE touching a key column      -> FOR UPDATE
--   UPDATE not touching a key column  -> FOR NO KEY UPDATE
--   DELETE                            -> always FOR UPDATE
--   foreign key check                 -> FOR KEY SHARE
--
-- The middle modes are not an advanced feature; they are what makes a schema with
-- foreign keys usable at all.
--
-- STAGING NOTE (caught on the first attempt): if T2 stays open, T3 blocks twice - on
-- T1's FOR KEY SHARE and on T2's FOR NO KEY UPDATE, both of which conflict with DELETE's
-- FOR UPDATE. T3 then stays blocked after T1 rolls back and the demo looks wrong.
-- The behavior is correct; the staging was not. T2 must finish first.
