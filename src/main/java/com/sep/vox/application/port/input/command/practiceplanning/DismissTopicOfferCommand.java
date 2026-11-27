package com.sep.vox.application.port.input.command.practiceplanning;

import java.util.UUID;

/** The student dismisses one topic card in the batch currently being offered. */
public record DismissTopicOfferCommand(UUID topicId) {
}
