package com.sep.vox.application.query.dto;

import java.util.UUID;

public interface TranscriptInfo {

    UUID getSessionId();

    String getTranscript();
}
