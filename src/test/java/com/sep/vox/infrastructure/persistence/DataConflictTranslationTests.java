package com.sep.vox.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import com.sep.vox.application.exception.DataConflictException;

class DataConflictTranslationTests {

    @Test
    void translating_supplier_returnsTheValueWhenNothingIsThrown() {
        String result = DataConflictTranslation.translating(() -> "saved");

        assertThat(result).isEqualTo("saved");
    }

    @Test
    void translating_supplier_turnsADataIntegrityViolationIntoTheCarrierKeepingTheCause() {
        DataIntegrityViolationException violation = new DataIntegrityViolationException("duplicate key");

        assertThatThrownBy(() -> DataConflictTranslation.translating(() -> {
            throw violation;
        }))
            .isInstanceOf(DataConflictException.class)
            .hasMessage("duplicate key")
            .hasCause(violation);
    }

    @Test
    void translating_runnable_turnsADataIntegrityViolationIntoTheCarrierKeepingTheCause() {
        DataIntegrityViolationException violation = new DataIntegrityViolationException("duplicate key");
        Runnable failingWork = () -> {
            throw violation;
        };

        assertThatThrownBy(() -> DataConflictTranslation.translating(failingWork))
            .isInstanceOf(DataConflictException.class)
            .hasCause(violation);
    }

    @Test
    void translating_letsEveryOtherExceptionThroughUnchanged() {
        IllegalStateException other = new IllegalStateException("other");
        Runnable failingWork = () -> {
            throw other;
        };

        assertThatThrownBy(() -> DataConflictTranslation.translating(failingWork)).isSameAs(other);
    }
}
