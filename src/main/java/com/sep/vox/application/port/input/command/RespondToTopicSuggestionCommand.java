package com.sep.vox.application.port.input.command;

import java.util.UUID;

public record RespondToTopicSuggestionCommand(UUID suggestionId, boolean accept) {
}
