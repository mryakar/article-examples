-- D03b - CREATE INDEX stops writes, CONCURRENTLY does not.   Section 2
-- First: psql -d b2 -f 00-setup.sql

-- ---------- Plain CREATE INDEX: takes SHARE, conflicts with ROW EXCLUSIVE ----------
-- T1: BEGIN;
--     CREATE INDEX idx_owner ON accounts(owner);
-- T2: INSERT INTO accounts VALUES (301, 'X', 0);      -- BLOCKED
-- T1: ROLLBACK;                                       -- T2 released

-- ---------- CONCURRENTLY: takes SHARE UPDATE EXCLUSIVE ----------
-- (cannot run inside a transaction block - no BEGIN)
-- T1: CREATE INDEX CONCURRENTLY idx_owner2 ON accounts(owner);
-- T2: INSERT INTO accounts VALUES (302, 'Y', 0);      -- does not wait

-- Observed: the INSERT blocked in the first case and did not in the second.
-- The reason production always uses CONCURRENTLY is one row in the conflict matrix.
--
-- Side note: SHARE UPDATE EXCLUSIVE conflicts with itself, so two CONCURRENTLY index
-- builds cannot run on the same table at the same time.
