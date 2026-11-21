package com.sep.vox.application.port.output;

import java.util.function.Supplier;

public interface AuditActorScopePort {

    /**
     * Runs the action with {@code actor} recorded as createdBy/updatedBy for every audited row it saves on the
     * current thread, for work that runs without a security context such as background imports. The previous actor
     * is restored when the action returns or throws.
     */
    <T> T callAs(String actor, Supplier<T> action);
}
