package com.sep.vox.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import com.sep.vox.domain.model.registerform.RegisterForm;
import com.sep.vox.domain.model.registerform.RegisterFormStatus;
import com.sep.vox.domain.repository.RegisterFormRepository;
import com.sep.vox.infrastructure.persistence.adapter.RegisterFormRepositoryImpl;
import com.sep.vox.support.RepositoryTest;

import jakarta.persistence.EntityManager;

/** Approving or rejecting is one guarded statement: the adapter stamps updated_at, the caller passes only the reviewer. */
@RepositoryTest
@Import(RegisterFormRepositoryImpl.class)
class RegisterFormReviewTests {

    @Autowired
    private RegisterFormRepository registerFormRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void whenApproved_thenTheStatusAndReviewerChangeAndUpdatedAtMoves() {
        UUID reviewerId = UUID.randomUUID();
        RegisterForm pending = savedPendingForm();

        int updatedRows = registerFormRepository.updateApprovedRegisterForm(pending.getId(), reviewerId);

        RegisterForm stored = reload(pending);
        assertThat(updatedRows).isEqualTo(1);
        assertThat(stored.getStatus()).isEqualTo(RegisterFormStatus.APPROVED);
        assertThat(stored.getReviewedBy()).isEqualTo(reviewerId);
        assertThat(stored.getUpdatedAt()).isAfterOrEqualTo(pending.getUpdatedAt().truncatedTo(ChronoUnit.MICROS));
    }

    @Test
    void whenRejected_thenTheReasonIsStoredAndASecondReviewChangesNothing() {
        RegisterForm pending = savedPendingForm();

        int firstRows = registerFormRepository.updateRejectedRegisterForm(pending.getId(), UUID.randomUUID(), "Incomplete");
        int secondRows = registerFormRepository.updateApprovedRegisterForm(pending.getId(), UUID.randomUUID());

        RegisterForm stored = reload(pending);
        assertThat(firstRows).isEqualTo(1);
        assertThat(secondRows).isZero();
        assertThat(stored.getStatus()).isEqualTo(RegisterFormStatus.REJECTED);
    }

    private RegisterForm savedPendingForm() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        RegisterForm form = RegisterForm.selfDeclared("School " + suffix, suffix + ".edu.vn", "1 Main Street", "Province",
            "District", "Contact Person", String.valueOf(ThreadLocalRandom.current().nextLong(100_000_000_000L, 999_999_999_999L)), suffix + "@example.com", "0900000000",
            LocalDate.of(1990, 1, 1), "Contact address", "700000", "Principal", 100, Instant.now().minusSeconds(60));
        return registerFormRepository.save(form);
    }

    private RegisterForm reload(RegisterForm form) {
        entityManager.flush();
        entityManager.clear();
        return registerFormRepository.findById(form.getId()).orElseThrow();
    }
}
