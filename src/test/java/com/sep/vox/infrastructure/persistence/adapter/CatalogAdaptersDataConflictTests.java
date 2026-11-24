package com.sep.vox.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import com.sep.vox.application.exception.DataConflictException;
import com.sep.vox.domain.model.gradelevel.GradeLevel;
import com.sep.vox.domain.model.gradelevel.GradeLevelStatus;
import com.sep.vox.domain.model.school.SchoolClassUser;
import com.sep.vox.domain.model.school.SchoolGrade;
import com.sep.vox.domain.model.supportedlanguage.SupportedLanguage;
import com.sep.vox.domain.valueobject.LanguageCode;
import com.sep.vox.infrastructure.persistence.audit.AuditStampProvider;
import com.sep.vox.infrastructure.persistence.repository.SpringDataGradeLevelRepository;
import com.sep.vox.infrastructure.persistence.repository.SpringDataSchoolClassUserRepository;
import com.sep.vox.infrastructure.persistence.repository.SpringDataSchoolGradeRepository;
import com.sep.vox.infrastructure.persistence.repository.SpringDataSupportedLanguageRepository;

/**
 * Every Spring Data call below answers with a constraint violation. The adapter must translate it into the carrier
 * exception that the use cases catch.
 */
class CatalogAdaptersDataConflictTests {

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
    void gradeLevel_translatesTheConstraintViolation() {
        GradeLevelRepositoryImpl adapter = new GradeLevelRepositoryImpl(repositoryRejectingEveryCall(SpringDataGradeLevelRepository.class), auditStampProvider);
        assertCarrier(() -> adapter.save(GradeLevel.builder().status(GradeLevelStatus.ACTIVE).build()));
        assertCarrier(() -> adapter.updateGradeLevelAtomic(UUID.randomUUID(), "name", "description", true, 1));
    }

    @Test
    void supportedLanguage_translatesTheConstraintViolation() {
        SupportedLanguageRepositoryImpl adapter = new SupportedLanguageRepositoryImpl(repositoryRejectingEveryCall(SpringDataSupportedLanguageRepository.class), auditStampProvider);
        assertCarrier(() -> adapter.save(SupportedLanguage.builder().code(new LanguageCode("EN")).build()));
        assertCarrier(() -> adapter.updateMutableFields(UUID.randomUUID(), "EN", true, "English", true, null, false, true, true));
    }

    @Test
    void schoolGrade_translatesTheConstraintViolation() {
        SchoolGradeRepositoryImpl adapter = new SchoolGradeRepositoryImpl(repositoryRejectingEveryCall(SpringDataSchoolGradeRepository.class), auditStampProvider);
        assertCarrier(() -> adapter.save(SchoolGrade.builder().build()));
    }

    @Test
    void schoolClassUser_translatesTheConstraintViolation() {
        SchoolClassUserRepositoryImpl adapter = new SchoolClassUserRepositoryImpl(repositoryRejectingEveryCall(SpringDataSchoolClassUserRepository.class));
        assertCarrier(() -> adapter.save(SchoolClassUser.builder().build()));
        assertCarrier(() -> adapter.saveAll(List.of(SchoolClassUser.builder().build())));
    }
}
