package com.sep.vox.application.port.output;

/**
 * Checks whether the AI service is ready to accept a speaking-practice session. Implemented by
 * PracticeAgentAvailabilityClient.
 */
public interface PracticeAgentAvailabilityPort {

    /**
     * Throws IllegalStateException if the AI service is not ready -- does NOT degrade gracefully
     * like the other AI ports, because entering a session while the agent is not alive makes the
     * student wait for nothing, so it is better to block right from the start.
     */
    void requireReady();
}
