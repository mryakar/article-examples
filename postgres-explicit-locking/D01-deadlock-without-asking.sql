-- D01 - a deadlock without writing a single lock.   Section 1 (hook) + section 7 (answer)
-- Two terminals. Run the steps IN ORDER, by step number.
-- First: psql -d b2 -f 00-setup.sql

-- ==================== T1 (terminal 1) ====================
-- step 1
\set VERBOSITY verbose
BEGIN;
-- step 2
UPDATE accounts SET balance = balance - 100 WHERE id = 100;   -- locked row 100
-- step 5   (AFTER T2 has done step 4)
UPDATE accounts SET balance = balance + 100 WHERE id = 200;   -- WAITS
-- step 8
COMMIT;

-- ==================== T2 (terminal 2) ====================
-- step 3
\set VERBOSITY verbose
BEGIN;
-- step 4
UPDATE accounts SET balance = balance - 100 WHERE id = 200;   -- locked row 200
-- step 6   (while T1 is waiting in step 5)
UPDATE accounts SET balance = balance + 100 WHERE id = 100;   -- CYCLE -> 40P01
-- step 7
ROLLBACK;

-- EXPECTED (observed on PostgreSQL 18.6):
--   ERROR:  deadlock detected
--   DETAIL: Process A waits for ShareLock on transaction X; blocked by process B.
--           Process B waits for ShareLock on transaction Y; blocked by process A.
--   CONTEXT: while updating tuple (0,1) in relation "accounts"
--   SQLSTATE: 40P01
--
-- The surviving transaction is released and completes.
-- Nowhere did we write FOR UPDATE or LOCK TABLE. Two ordinary UPDATEs were enough.
