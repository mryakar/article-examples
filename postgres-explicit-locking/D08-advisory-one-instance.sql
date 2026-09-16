-- D08 - "only one instance in the cluster should run this job".   Section 8
-- THREE terminals = three instances. No setup needed.

-- A: SELECT pg_try_advisory_lock(42);   -->  t   (runs the job)
-- B: SELECT pg_try_advisory_lock(42);   -->  f   (skips, WITHOUT waiting)
-- C: SELECT pg_try_advisory_lock(42);   -->  f   (skips, WITHOUT waiting)
-- A: SELECT pg_advisory_unlock(42);     -->  t
-- B: SELECT pg_try_advisory_lock(42);   -->  t   (now B gets it)
-- B: SELECT pg_advisory_unlock(42);

-- The TRY variant is the point: the loser does not queue, it skips the round.
-- Flyway's migration lock and ShedLock are the same mechanism.
-- On a crash the session dies, the lock is released, and the next trigger finds it free.
-- That is exactly what a job_running flag in a table cannot do.

-- ---------- Reentrant: the same session always gets it ----------
-- A: SELECT pg_try_advisory_lock(42);   --> t
-- A: SELECT pg_try_advisory_lock(42);   --> t    <-- SAME session, t again
-- A: SELECT locktype, objid, mode FROM pg_locks WHERE locktype = 'advisory';
--      --> ONE row. How many times it is held is NOT visible in pg_locks.
-- A: SELECT pg_advisory_unlock(42);     --> t   but the lock is STILL held
--      (another session still gets f at this point)
-- A: SELECT pg_advisory_unlock(42);     --> t   now it is released
--
-- N acquires need N releases, and the counter appears in no system view.
-- These two facts together are what D11 is built on.
