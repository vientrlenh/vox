package com.sep.vox.domain.common;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Ép kiểu cho các ô {@code Object[]} mà native query trả về.
 *
 * <p>Tồn tại vì cùng một cột về Java dưới nhiều kiểu khác nhau tùy phiên bản driver và Hibernate:
 * {@code COUNT(*)} là {@code Long} hoặc {@code BigInteger}, cột {@code timestamptz} là
 * {@link Instant}, {@link OffsetDateTime} hoặc {@link java.sql.Timestamp}. Ép thẳng sang một kiểu cụ
 * thể chạy được trên máy này và ném {@code ClassCastException} trên máy khác.
 *
 * <p>Gói riêng cho tầng native query. Câu JPQL không cần tới đây — Hibernate đã trả về đúng kiểu của
 * thuộc tính entity.
 */
public final class NativeQueryValues {

    private NativeQueryValues() {
    }

    public static long toLong(Object value) {
        return value == null ? 0L : ((Number) value).longValue();
    }

    /** Số đếm luôn nằm gọn trong {@code int} ở mọi màn hình — nhưng đọc về vẫn phải qua Number. */
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
        throw new IllegalStateException("Không đọc được cột thời gian kiểu " + value.getClass());
    }
}
