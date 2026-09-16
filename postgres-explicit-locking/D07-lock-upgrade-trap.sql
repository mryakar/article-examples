-- D07 - the lock upgrade trap: both sides behave well, both die.   Section 7
-- First: psql -d b2 -f 00-setup.sql

-- ==================== T1 ====================
BEGIN;
SELECT id FROM accounts WHERE id = 100 FOR SHARE;    -- step 2, shared
SELECT id FROM accounts WHERE id = 100 FOR UPDATE;   -- step 4, upgrading -> waits
ROLLBACK;

-- ==================== T2 ====================
BEGIN;
SELECT id FROM accounts WHERE id = 100 FOR SHARE;    -- step 3, fine, both hold it
SELECT id FROM accounts WHERE id = 100 FOR UPDATE;   -- step 5, also upgrading -> CYCLE
ROLLBACK;

-- OBSERVED:
--   ERROR:  deadlock detected
--   CONTEXT: while LOCKING tuple (0,1) in relation "accounts"
--                  ^^^^^^^ D01 said "while UPDATING" - the CONTEXT line tells you
--                          which operation the collision happened in.
--
-- RULE: take the most restrictive mode you will need, up front.
-- If both had taken FOR UPDATE from the start, one would simply have waited.
-- This is a different trap from D01: there were two rows there, one row here.
-- Lock ordering does NOT fix this case - the order is already the same. The fix is
-- the mode you choose.
