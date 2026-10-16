package dev.horizon.basepoint;

/**
 * How a query locks the rows that it reads, until its transaction ends. Each dialect writes the lock in the syntax of
 * its database.
 */
public enum LockMode {
  /** Locks the rows against the updates and deletes of other transactions, which can still read them. */
  SHARE,
  /** Locks the rows to update: other transactions wait to lock, update or delete them. */
  UPDATE,
  /** Locks the rows to update, and leaves out the rows that another transaction locked. */
  UPDATE_SKIP_LOCKED,
  /** Locks the rows to update, and fails at once if another transaction locked one. */
  UPDATE_NOWAIT
}
