-- D10 - pg_locks: who is waiting for whom.   Section 10 (the diagnosis section)
-- Set up a wait first (the last step of D05 works), then run these from a THIRD session.

-- 1) Who is waiting, and on what
SELECT pid, state, wait_event_type, wait_event, left(query, 46) AS query
  FROM pg_stat_activity
 WHERE datname = current_database() AND pid <> pg_backend_pid()
 ORDER BY pid;
-- waiter : wait_event_type = 'Lock', wait_event = 'transactionid', state = 'active'
-- blocker: state = 'idle in transaction'   <-- the warning sign; nobody is running SQL
--          and locks are held anyway.

-- 2) The waiter -> blocker chain, the long way
SELECT w.pid AS waiting, b.pid AS blocking,
       w.mode AS requested_mode, b.mode AS held_mode, w.locktype
  FROM pg_locks w
  JOIN pg_locks b
    ON w.locktype = b.locktype
   AND w.transactionid IS NOT DISTINCT FROM b.transactionid
   AND w.pid <> b.pid AND b.granted
 WHERE NOT w.granted;
-- Observed: the waiter wants ShareLock, the blocker holds ExclusiveLock, and
-- locktype = 'transactionid'. A wait for a row is recorded as a wait for the other
-- transaction to finish.

-- 3) The shortcut
SELECT pid, pg_blocking_pids(pid) AS blocking_pids
  FROM pg_stat_activity
 WHERE cardinality(pg_blocking_pids(pid)) > 0;

-- 4) Advisory locks
SELECT l.pid, l.objid, l.mode, l.granted, a.state, a.application_name
  FROM pg_locks l LEFT JOIN pg_stat_activity a USING (pid)
 WHERE l.locktype = 'advisory';
-- WARNING (observed): how many TIMES a session holds the lock is not visible here -
-- a lock taken three times still shows as a single row. That is what makes the ghost
-- lock in D11 hard to diagnose.
