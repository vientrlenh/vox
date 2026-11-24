package com.sep.vox.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import com.sep.vox.application.exception.DataConflictException;
import com.sep.vox.domain.model.rubric.RubricCriterion;
import com.sep.vox.domain.model.rubric.RubricResultBand;
import com.sep.vox.infrastructure.persistence.audit.AuditStampProvider;
import com.sep.vox.infrastructure.persistence.repository.SpringDataRubricCriterionRepository;
import com.sep.vox.infrastructure.persistence.repository.SpringDataRubricResultBandRepository;
import com.sep.vox.infrastructure.persistence.repository.SpringDataRubricVersionRepository;

/**
 * Every Spring Data call below answers with a constraint violation. The adapter must translate it into the carrier
 * exception that the use cases catch.
 */
class RubricAdaptersDataConflictTests {

    private static final DataIntegrityViolationException VIOLATION = new DataIntegrityViolationException("unique violation");

    private final AuditStampProvider auditStampProvider = mock(AuditStampProvider.class);

    private static <T> T repositoryRejectingEveryCall(Class<T> repositoryType) {
        return mock(repositoryType, invocation -> {
            throw VIOLATION;
        });
    }

    private static void assertCarrier(org.assertj.core.api.ThrowableAssert.ThrowingCallable call) {
        assertThatThrownBy(call).isInstanceOf(DataConflictException.class).hasCause(VIOLATION);
    }


    @Test
    void rubricCriterion_translatesTheConstraintViolation() {
        RubricCriterionRepositoryImpl adapter = new RubricCriterionRepositoryImpl(repositoryRejectingEveryCall(SpringDataRubricCriterionRepository.class), auditStampProvider);
        assertCarrier(() -> adapter.saveAll(List.of(RubricCriterion.builder().build())));
        assertCarrier(() -> adapter.updateCriterionAtomic(UUID.randomUUID(), null, "name", "description", true, null, true, BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.TEN, 1, true));
    }

    @Test
    void rubricResultBand_translatesTheConstraintViolation() {
        RubricResultBandRepositoryImpl adapter = new RubricResultBandRepositoryImpl(repositoryRejectingEveryCall(SpringDataRubricResultBandRepository.class), auditStampProvider);
        assertCarrier(() -> adapter.saveAll(List.of(RubricResultBand.builder().build())));
        assertCarrier(() -> adapter.updateResultBandAtomic(UUID.randomUUID(), null, "name", "description", true, BigDecimal.ZERO, BigDecimal.TEN, 1));
    }

    @Test
    void rubricVersion_translatesTheConstraintViolation() {
        RubricVersionRepositoryImpl adapter = new RubricVersionRepositoryImpl(repositoryRejectingEveryCall(SpringDataRubricVersionRepository.class), auditStampProvider);
        assertCarrier(() -> adapter.updateRubricVersionAtomic(UUID.randomUUID(), null, "name", "description", true, Instant.now(), null, true, BigDecimal.ZERO, BigDecimal.TEN, "AVERAGE"));
    }
}
