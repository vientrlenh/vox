package com.sep.vox.application.port.input.command;

import java.util.UUID;

/** Học sinh bấm loại một thẻ chủ đề trong lô đang được chào. */
public record DismissTopicOfferCommand(UUID topicId) {
}
