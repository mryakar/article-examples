-- D09 - a session-level lock survives ROLLBACK. An xact-level one does not.  Section 8
-- No setup needed.

-- ---------- Session level ----------
-- A: BEGIN;
-- A: SELECT pg_advisory_lock(77);
-- A: ROLLBACK;
-- A: SELECT count(*) FROM pg_locks WHERE locktype = 'advisory';   -->  1   (!)
-- B: SELECT pg_try_advisory_lock(77);                             -->  f   (still held)
-- A: SELECT pg_advisory_unlock(77);

-- ---------- Transaction level ----------
-- A: BEGIN;
-- A: SELECT pg_advisory_xact_lock(88);
-- A: ROLLBACK;
-- A: SELECT count(*) FROM pg_locks WHERE locktype = 'advisory';   -->  0

-- Both observed.
--
-- The decision is one question: does the protected work fit in one transaction?
--   Yes -> pg_advisory_xact_lock   (THE SAFE DEFAULT)
--          released automatically, released on ROLLBACK, no unlock code = no unlock bug
--   No  -> session level is unavoidable
--          e.g. a long batch that commits as it goes has to hold the "one instance"
--          lock across those commits; an xact-level lock would drop at the first commit
--          the price: unlock in a finally, plus the pooled-connection trap in D11
