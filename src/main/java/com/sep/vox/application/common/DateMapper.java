package com.sep.vox.application.common;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

import com.sep.vox.domain.common.ZoneConstant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

public final class DateMapper {

    private DateMapper() {
    }

    private static final List<String> INPUT_LOCALDATE_FORMAT = List.of(
            "yyyy-MM-dd",
            "yyyy/MM/dd",
            "dd-MM-yyyy",
            "dd/MM/yyyy"
    );

    /**
     * Formatter built once. {@link DateTimeFormatter#ofPattern} has to recompile the pattern string
     * on every call, so creating it inside the parse loop makes every request pay that cost.
     */
    private static final List<DateTimeFormatter> INPUT_LOCALDATE_FORMATTERS =
            INPUT_LOCALDATE_FORMAT.stream().map(DateTimeFormatter::ofPattern).toList();

    private static final DateTimeFormatter OUTPUT_LOCALDATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd-MM-yyyy");

    /**
     * The system's business time zone. Used for two things:
     *
     * <ul>
     *   <li><b>Interpreting input that carries no offset</b> -- see {@link #toInstant(String)} and
     *       {@link #toImportedInstant(String, ZoneId)}.
     *   <li><b>Showing times to users</b> (mail, CSV, date strings). An {@link Instant} cannot be
     *       formatted by itself -- it carries no time zone, so the formatting code must be given a
     *       zone. Do not give it the JVM default zone: that is only correct today because the
     *       {@code Dockerfile} sets {@code TZ=Asia/Ho_Chi_Minh} and {@code build.gradle} sets
     *       {@code user.timezone} for the {@code test} task -- change either one and the displayed
     *       times shift without any test turning red. An explicit zone does not depend on them.
     * </ul>
     *
     * <p>Deliberately {@code public}: every place that needs it must pass it by hand to
     * {@link #toImportedInstant(String, ZoneId)} or {@code DateTimeFormatter.withZone(...)}, so the
     * choice of zone is visible at the call site and not hidden inside the parser/formatter. When
     * {@code School} gets its own time zone column, grepping this constant gives exactly the list
     * of places to switch to the school's zone.
     *
     * <p>Note when removing the compatibility branch in {@link #toInstant(String)}: this constant
     * does <b>not</b> go away with it, because the import path and the display path still need it.
     *
     * <p>Uses {@link ZoneId} rather than {@code ZoneOffset.ofHours(7)} because {@code atStartOfDay}
     * needs a ZoneId, and a fixed offset would be wrong in regions with daylight saving time.
     *
     * <p>The real value lives in {@link ZoneConstant#BUSINESS_ZONE}: domain models need exactly this
     * zone too (e.g. {@code SubscriptionPlan.endDateFrom}) but the domain must not depend on the
     * application layer, so the source of truth has to sit in the inner layer. The old name is kept
     * here so that no call site has to change.
     */
    public static final ZoneId DEFAULT_INPUT_ZONE = ZoneConstant.BUSINESS_ZONE;

    public static LocalDate toLocalDate(String localDateString) {
        if (localDateString == null || localDateString.isBlank()) {
            return null;
        }
        String stripped = localDateString.strip();
        for (DateTimeFormatter formatter : INPUT_LOCALDATE_FORMATTERS) {
            try {
                return LocalDate.parse(stripped, formatter);
            } catch (DateTimeParseException e) {
                // Try the next pattern.
            }
        }
        throw new IllegalArgumentException("The requested date format is invalid");
    }

    public static String localDateToString(LocalDate date) {
        return date.format(OUTPUT_LOCALDATE_FORMATTER);
    }

    /**
     * Converts a client date-time string into an absolute instant.
     *
     * <p>The preferred input carries its own offset ({@code Z} or {@code ±HH:MM}): the server then
     * guesses nothing and a client in any time zone yields the right moment. That is why this method
     * calls {@link OffsetDateTime#parse} directly instead of matching a list of patterns:
     * {@code ISO_OFFSET_DATE_TIME} already accepts optional seconds, 0-9 fractional digits, {@code Z},
     * {@code ±HH:MM} and {@code ±HH:MM:SS}, which is wider than any hand-written {@code ofPattern}
     * list and cannot miss a shape a real client sends (for example
     * {@code 2026-07-28T09:30:00.000Z} from {@code new Date().toISOString()}).
     *
     * <p>Input without an offset is still accepted for now through the compatibility branch in
     * {@link #resolveWithoutOffsetForApi(String)}.
     *
     * @return {@code null} if the input is empty
     * @throws IllegalArgumentException if the input cannot be parsed in any form
     */
    public static Instant toInstant(String dateTimeString) {
        if (dateTimeString == null || dateTimeString.isBlank()) {
            return null;
        }
        String raw = dateTimeString.strip();
        Instant withOffset = parseWithOffset(raw);
        if (withOffset != null) {
            return withOffset;
        }
        return resolveWithoutOffsetForApi(raw);
    }

    /**
     * Converts a date/time cell read from an import file into an absolute instant.
     *
     * <p>It differs from {@link #toInstant(String)} in two ways, both on purpose:
     *
     * <ul>
     *   <li>A date-only value is <b>valid</b>, not technical debt. A user who types
     *       {@code 28/07/2026} into Excel gives no time zone to read, so there is nothing to warn
     *       about; this method therefore does not log like the API compatibility branch.
     *   <li>{@code zone} is a <b>required</b> parameter. A calendar date does not identify a moment:
     *       when "28/07 starts" depends entirely on the time zone. Passing it by hand keeps the
     *       choice visible at the call site instead of hidden in the parser, and lets a grep find
     *       every place to change when {@code School} gets its own time zone column.
     * </ul>
     *
     * <p>A cell that carries an offset ignores {@code zone}: the data is already exact.
     *
     * @return {@code null} if the cell is empty
     * @throws IllegalArgumentException if the cell cannot be parsed in any form
     */
    public static Instant toImportedInstant(String dateTimeString, ZoneId zone) {
        if (dateTimeString == null || dateTimeString.isBlank()) {
            return null;
        }
        String raw = dateTimeString.strip();
        Instant withOffset = parseWithOffset(raw);
        if (withOffset != null) {
            return withOffset;
        }
        Instant resolved = resolveWithoutOffset(raw, zone);
        if (resolved == null) {
            throw new IllegalArgumentException("The requested date format is invalid");
        }
        return resolved;
    }

    /**
     * Converts an absolute point in time to the string returned by the API.
     *
     * <p>Uses ISO-8601 in UTC (suffix {@code Z}) -- exactly what {@link Instant#toString()} already
     * produces, so calling this does not change the data clients receive. The difference is that
     * <b>the place that decides the format is now only one</b>: before, every mapper called
     * {@code .toString()} itself, so changing the display meant editing each one and it was easy to miss.
     *
     * <p>Deliberately NOT switched to {@link #DEFAULT_INPUT_ZONE}: the string returned by the API is
     * machine-readable data and the client converts it to the user's local time itself. Only the
     * paths shown to people (mail, CSV) need a zone -- see the javadoc of {@code DEFAULT_INPUT_ZONE}.
     *
     * @return {@code null} when {@code instant} is null, so the caller does not have to check
     */
    public static String toIsoString(Instant instant) {
        return instant == null ? null : instant.toString();
    }

    private static Instant parseWithOffset(String raw) {
        try {
            return OffsetDateTime.parse(raw).toInstant();
        } catch (DateTimeParseException notSelfDescribing) {
            return null;
        }
    }

    // TEMPORARY COMPATIBILITY BRANCH.
    //
    // The web client still sends date-only values from <input type="date"> (for example
    // "2026-07-28") for effectiveFrom/effectiveTo. Dropping this branch now would break creating and
    // editing assessment policies on both REST and GraphQL, so it exists to let the two sides deploy
    // at different times.
    //
    // REMOVE it once clients always send an offset: input without an offset will then be rejected
    // outright, and DEFAULT_INPUT_ZONE is left for the file import path only.
    private static Instant resolveWithoutOffsetForApi(String raw) {
        Instant fallback = resolveWithoutOffset(raw, DEFAULT_INPUT_ZONE);
        if (fallback == null) {
            throw new IllegalArgumentException(
                    "The time must include a time zone, for example 2026-07-28T09:30:00+07:00 or 2026-07-28T02:30:00Z");
        }
        return fallback;
    }

    /**
     * Interprets input that carries no offset according to {@code zone}: tries the form with a
     * time first, then the date-only form.
     *
     * @return {@code null} if neither form matches -- so the caller decides the error message
     */
    private static Instant resolveWithoutOffset(String raw, ZoneId zone) {
        try {
            return LocalDateTime.parse(raw).atZone(zone).toInstant();
        } catch (DateTimeParseException e) {
            // Try the date-only form.
        }
        try {
            LocalDate date = toLocalDate(raw);
            return date == null ? null : date.atStartOfDay(zone).toInstant();
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
