-- D05 - row locks do not block readers.   Section 5
-- First: psql -d b2 -f 00-setup.sql

-- ==================== T1 ====================
BEGIN;
SELECT id, balance FROM accounts WHERE id = 100 FOR UPDATE;   -- row 100 is locked
-- ... after T2's steps ...
ROLLBACK;

-- ==================== T2 ====================
BEGIN;
SELECT id, owner, balance FROM accounts WHERE id = 100;   -- SAME row      -> no wait
SELECT count(*) FROM accounts;                            -- full scan     -> no wait
UPDATE accounts SET balance = 1 WHERE id = 200;           -- ANOTHER row   -> no wait
UPDATE accounts SET balance = 1 WHERE id = 100;           -- SAME row      -> WAITS
ROLLBACK;

-- Observed: the first three did not wait, the fourth blocked, and it was released
-- when T1 rolled back.
--
-- Lock granularity is contention granularity. In a table of a million accounts,
-- locking row 100 leaves the other 999,999 untouched, and even a SELECT showing
-- account 100's balance goes straight through.
-- The only one who waits is a writer on the same row.
