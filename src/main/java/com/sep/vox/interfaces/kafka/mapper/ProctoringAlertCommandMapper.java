package com.sep.vox.interfaces.kafka.mapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

import com.sep.vox.application.common.DateMapper;
import com.sep.vox.application.port.input.command.proctoring.RecordProctoringAlertCommand;
import com.sep.vox.interfaces.kafka.dto.AlertRaisedEventDto;

public final class ProctoringAlertCommandMapper {

    /** Matches the column {@code detail varchar(1024)}. Truncating beats letting the whole alert fall into the DLT. */
    private static final int MAX_DETAIL_LENGTH = 1024;

    private ProctoringAlertCommandMapper() {
    }

    public static RecordProctoringAlertCommand toCommand(AlertRaisedEventDto dto) {
        Instant capturedAt = DateMapper.toInstant(dto.capturedAt());
        Instant raisedAt = DateMapper.toInstant(dto.raisedAt());

        return new RecordProctoringAlertCommand(
            requireEventId(dto.eventId()),
            UUID.fromString(requireText(dto.sessionId(), "sessionId")),
            toOptionalUuid(dto.participantId()),
            trimToNull(dto.streamId()),
            upperOrNull(dto.streamType()),
            requireText(dto.alertType(), "alertType").trim(),
            upperOrNull(dto.level()),
            upperOrNull(dto.source()),
            truncate(trimToNull(dto.detail())),
            toConfidence(dto.confidence()),
            dto.sequenceNo(),
            capturedAt == null ? Instant.now() : capturedAt,
            raisedAt == null ? Instant.now() : raisedAt
        );
    }

    /**
     * eventId is the key that prevents duplicate writes, so without it nothing can prevent duplicates.
     *
     * <p>It is not generated here instead: a new id on every redelivery would turn each Kafka redelivery into a
     * new row, and the violation counter would swell with infrastructure incidents rather than with the
     * candidate's behaviour. Better to reject the message than to write a number nobody can trust.
     */
    private static String requireEventId(String raw) {
        return requireText(raw, "eventId").trim();
    }

    private static String requireText(String raw, String field) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Proctoring alert is missing a required field: " + field);
        }
        return raw;
    }

    /**
     * candidateId, if the sender knows it. Empty or not a UUID returns null.
     *
     * <p>The AI side once set this field to the assessment session id itself, so old data like that is still on the topic.
     * No attempt is made to fix it here: a wrong value is still a valid UUID so it cannot be told apart from
     * here, and the back-fill is already done in a better place -- right in vox-streaming, the only place that knows
     * the mapping from assessment session -> candidate.
     */
    private static UUID toOptionalUuid(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(raw.trim());
        } catch (IllegalArgumentException notAUuid) {
            return null;
        }
    }

    /**
     * Confidence on a [0, 1] scale with 4 decimal places, matching {@code numeric(5,4)}.
     *
     * <p>Out of range returns null rather than clamping to the edge: an absurd value means we do not know the
     * confidence, and "unknown" must look different from "100% sure".
     */
    private static BigDecimal toConfidence(Double raw) {
        if (raw == null || raw.isNaN() || raw.isInfinite()) {
            return null;
        }
        BigDecimal value = BigDecimal.valueOf(raw).setScale(4, RoundingMode.HALF_UP);
        if (value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(BigDecimal.ONE) > 0) {
            return null;
        }
        return value;
    }

    private static String truncate(String raw) {
        if (raw == null || raw.length() <= MAX_DETAIL_LENGTH) {
            return raw;
        }
        return raw.substring(0, MAX_DETAIL_LENGTH);
    }

    private static String upperOrNull(String raw) {
        String trimmed = trimToNull(raw);
        return trimmed == null ? null : trimmed.toUpperCase(Locale.ROOT);
    }

    private static String trimToNull(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
