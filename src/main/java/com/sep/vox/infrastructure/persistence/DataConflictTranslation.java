package com.sep.vox.infrastructure.persistence;

import java.util.function.Supplier;

import org.springframework.dao.DataIntegrityViolationException;

import com.sep.vox.application.exception.DataConflictException;

/**
 * Turns Spring's {@link DataIntegrityViolationException} into the application's {@link DataConflictException},
 * keeping the original as the cause. Every other exception passes through unchanged.
 */
public final class DataConflictTranslation {

    private DataConflictTranslation() {
    }

    public static <T> T translating(Supplier<T> work) {
        try {
            return work.get();
        } catch (DataIntegrityViolationException violation) {
            throw new DataConflictException(violation.getMessage(), violation);
        }
    }

    public static void translating(Runnable work) {
        translating(() -> {
            work.run();
            return null;
        });
    }
}
