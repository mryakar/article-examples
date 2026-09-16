-- D02 - lock ordering: no cycle can form, only a queue.   Section 7
-- First: psql -d b2 -f 00-setup.sql

-- ==================== T1 ====================
-- step 1
BEGIN;
-- step 2 - locks both rows in the SAME order (lowest id first)
SELECT id, owner FROM accounts WHERE id IN (100, 200) ORDER BY id FOR UPDATE;
-- step 4
UPDATE accounts SET balance = balance - 100 WHERE id = 100;
UPDATE accounts SET balance = balance + 100 WHERE id = 200;
-- step 5
COMMIT;

-- ==================== T2 ====================
-- step 3 - same query, same order: blocks at the FIRST row
SELECT id, owner FROM accounts WHERE id IN (100, 200) ORDER BY id FOR UPDATE;
-- released when T1 commits, then takes both rows.
COMMIT;

-- THE POINT: while T2 waits it HOLDS NOTHING.
-- The second edge of the cycle never exists, so deadlock is structurally impossible.
-- This is the SQL twin of picking first/second by compareTo in the Java article.
