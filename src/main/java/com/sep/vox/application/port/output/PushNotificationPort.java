package com.sep.vox.application.port.output;

import java.util.List;

import com.sep.vox.application.response.output.PushDispatchResult;
import com.sep.vox.application.response.output.PushMessage;

public interface PushNotificationPort {

    /**
     * Sends one notification to many devices. Splits into batches by the provider's limit itself,
     * so the caller may pass any number of FIDs.
     *
     * <p>Does not throw when some devices fail -- a failure on an individual device is normal
     * (app removed, device changed) and is returned in the result so the caller can clean up.
     */
    PushDispatchResult send(PushMessage message, List<String> installationIds);

    /** Whether push is enabled. Lets the caller skip building the content when it is off. */
    boolean isEnabled();
}
