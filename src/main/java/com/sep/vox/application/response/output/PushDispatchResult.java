package com.sep.vox.application.response.output;

import java.util.ArrayList;
import java.util.List;

/**
 * The push result, with failures already classified by the action the caller needs to take.
 *
 * <p>The classification is here and not left to the caller to read FCM error codes, because this is the place
 * that can lose data: if everything were merged into "delete the device on any error", one FCM downtime
 * (mass UNAVAILABLE) would wipe the notification_devices table and every user
 * would have to register their device again from scratch.
 *
 * @param successCount               the number of devices that received the notification
 * @param staleInstallationIds       FIDs that are certainly no longer usable -- the caller SHOULD delete them from the DB
 * @param retryableInstallationIds   FIDs with a temporary failure -- the caller MUST keep them and retry later
 */
public record PushDispatchResult(
    int successCount,
    List<String> staleInstallationIds,
    List<String> retryableInstallationIds
) {
    private static final PushDispatchResult EMPTY = new PushDispatchResult(0, List.of(), List.of());

    public PushDispatchResult {
        staleInstallationIds = staleInstallationIds == null ? List.of() : List.copyOf(staleInstallationIds);
        retryableInstallationIds = retryableInstallationIds == null ? List.of() : List.copyOf(retryableInstallationIds);
    }

    public static PushDispatchResult empty() {
        return EMPTY;
    }

    public int failureCount() {
        return staleInstallationIds.size() + retryableInstallationIds.size();
    }

    /** Merges the results of the batches of 500 into a single result for the caller. */
    public PushDispatchResult merge(PushDispatchResult other) {
        if (other == null) {
            return this;
        }

        List<String> stale = new ArrayList<>(this.staleInstallationIds);
        stale.addAll(other.staleInstallationIds);

        List<String> retryable = new ArrayList<>(this.retryableInstallationIds);
        retryable.addAll(other.retryableInstallationIds);

        return new PushDispatchResult(this.successCount + other.successCount, stale, retryable);
    }
}
