package com.sep.vox.domain.common;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Casts the {@code Object[]} cells returned by a native query.
 *
 * <p>Exists because the same column arrives in Java under different types depending on the driver and Hibernate
 * version: {@code COUNT(*)} is {@code Long} or {@code BigInteger}, a {@code timestamptz} column is
 * {@link Instant}, {@link OffsetDateTime} or {@link Timestamp}. Casting straight to one specific type works on
 * this machine and throws {@code ClassCastException} on another.
 *
 * <p>Scoped to the native query layer. JPQL queries do not need it — Hibernate already returns the type of the
 * entity attribute.
 */
public final class NativeQueryValues {

    private NativeQueryValues() {
    }

    public static long toLong(Object value) {
        return value == null ? 0L : ((Number) value).longValue();
    }

    /** A count always fits in an {@code int} on every screen — but it must still be read through Number. */
    public static int toInt(Object value) {
        return value == null ? 0 : ((Number) value).intValue();
    }

    public static UUID toUuid(Object value) {
        return value == null ? null : (UUID) value;
    }

    public static Instant toInstant(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Instant instant) {
            return instant;
        }
        if (value instanceof OffsetDateTime offsetDateTime) {
            return offsetDateTime.toInstant();
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toInstant();
        }
        throw new IllegalStateException("Cannot read a time column of type " + value.getClass());
    }
}
