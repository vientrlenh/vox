package com.sep.vox.application.event;

import java.util.UUID;

/**
 * Payload of the UserCreated event on Kafka.
 *
 * Does NOT carry the set-password token: the token is a credential, and in the payload it would
 * exist as plaintext in the outboxes table and in the Kafka topic (whose retention is usually
 * longer than the token's 48h lifetime). The consumer generates the token right before sending the mail.
 */
public record UserCreatedPayloadV1(
    UUID userId,
    String to,
    String fullName,
    String schoolName,
    String userType
) {

}
