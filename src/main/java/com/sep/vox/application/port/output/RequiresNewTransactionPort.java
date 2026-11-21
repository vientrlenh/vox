package com.sep.vox.application.port.output;

import java.util.function.Supplier;

/**
 * Runs work in its own transaction, separate from the caller's (if any), so the work is committed or rolled back
 * independently of the enclosing transaction. An exception thrown by the work rolls back only that inner
 * transaction and is rethrown to the caller unchanged, except that a database constraint violation (also one raised
 * when the inner transaction commits) is rethrown as {@link com.sep.vox.application.exception.DataConflictException}.
 */
public interface RequiresNewTransactionPort {

    void runInNewTransaction(Runnable work);

    <T> T callInNewTransaction(Supplier<T> work);
}
