-- D06 - same SQL, different isolation level, different outcome.   Section 6
-- Run three times: READ COMMITTED / REPEATABLE READ / SERIALIZABLE
-- Before each round: psql -d b2 -f 00-setup.sql

-- ==================== T1 ====================
-- step 1 - use each of the three levels in turn
BEGIN ISOLATION LEVEL READ COMMITTED;
-- step 2 - the snapshot is fixed here (critical for RR/SER; skip it and the demo fails)
SELECT balance FROM accounts WHERE id = 100;
-- step 5
SELECT id, balance FROM accounts WHERE id = 100 FOR UPDATE;   -- WAITS
-- step 7
ROLLBACK;

-- ==================== T2 ====================
-- step 3
BEGIN;
-- step 4
UPDATE accounts SET balance = balance - 500 WHERE id = 100;
-- step 6
COMMIT;

-- OBSERVED:
--   READ COMMITTED  -> T1 waits, then locks the NEW version (500.00). No error, no retry.
--   REPEATABLE READ -> ERROR: could not serialize access due to concurrent update
--   SERIALIZABLE    -> the same error
--
-- In one line: Read Committed adapts, Repeatable Read gives up.
-- That is why "Read Committed + FOR UPDATE" (pessimistic) and "Serializable + retry"
-- (optimistic) are two separate coherent strategies, and mixing them pays both costs.
