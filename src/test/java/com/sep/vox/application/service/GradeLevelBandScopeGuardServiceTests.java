package com.sep.vox.application.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.port.input.service.GradeLevelBandScopeGuardService;
import com.sep.vox.domain.model.framework.FrameworkResultBand;
import com.sep.vox.domain.model.gradelevel.GradeLevel;
import com.sep.vox.domain.model.gradelevel.GradeLevelBandScope;
import com.sep.vox.domain.model.school.SchoolClass;
import com.sep.vox.domain.model.school.SchoolGrade;
import com.sep.vox.domain.repository.FrameworkResultBandRepository;
import com.sep.vox.domain.repository.GradeLevelBandScopeRepository;
import com.sep.vox.domain.repository.GradeLevelRepository;
import com.sep.vox.domain.repository.SchoolClassRepository;
import com.sep.vox.domain.repository.SchoolGradeRepository;

/**
 * The two key points of this suite:
 *
 * <p>1. The band ceiling must hold EVEN WHEN the Assessment Policy picks a scope narrower than Grade. A policy must
 * choose exactly 1 of 3 scopes (Class | School-year grade | Grade), so if we only guard when the scope is
 * Grade then picking the Class scope makes the ceiling lose its effect by itself.
 *
 * <p>2. The ceiling differs by KIND OF ASSESSMENT: CENTRALIZE (Grade/School-year scope, not anchored to
 * 1 specific Class) only gets the low ceiling; CLASS_TEST (scope anchored to exactly 1 Class) gets the higher ceiling.
 */
class GradeLevelBandScopeGuardServiceTests {

    private static final UUID GRADE_LEVEL_ID = UUID.randomUUID();
    private static final UUID FRAMEWORK_VERSION_ID = UUID.randomUUID();
    private static final UUID SCHOOL_GRADE_ID = UUID.randomUUID();
    private static final UUID SCHOOL_CLASS_ID = UUID.randomUUID();
    private static final UUID CENTRALIZE_CAP_BAND_ID = UUID.randomUUID();
    private static final UUID CLASS_TEST_CAP_BAND_ID = UUID.randomUUID();

    private GradeLevelBandScopeRepository bandScopeRepository;
    private FrameworkResultBandRepository frameworkResultBandRepository;
    private SchoolGradeRepository schoolGradeRepository;
    private SchoolClassRepository schoolClassRepository;
    private GradeLevelBandScopeGuardService guard;

    @BeforeEach
    void setUp() {
        bandScopeRepository = mock(GradeLevelBandScopeRepository.class);
        frameworkResultBandRepository = mock(FrameworkResultBandRepository.class);
        schoolGradeRepository = mock(SchoolGradeRepository.class);
        schoolClassRepository = mock(SchoolClassRepository.class);
        GradeLevelRepository gradeLevelRepository = mock(GradeLevelRepository.class);

        GradeLevel gradeLevel = mock(GradeLevel.class);
        when(gradeLevel.getName()).thenReturn("Khối 10");
        when(gradeLevelRepository.findById(GRADE_LEVEL_ID)).thenReturn(Optional.of(gradeLevel));

        guard = new GradeLevelBandScopeGuardService(bandScopeRepository, frameworkResultBandRepository,
                gradeLevelRepository, schoolGradeRepository, schoolClassRepository);
    }

    /** CENTRALIZE ceiling = band order 4 ("B2"); CLASS_TEST ceiling = band order 5 ("C1"). */
    private void stubCaps() {
        GradeLevelBandScope scope = mock(GradeLevelBandScope.class);
        when(scope.getDefaultTargetBandId()).thenReturn(CENTRALIZE_CAP_BAND_ID);
        when(scope.getHardMaxBandId()).thenReturn(CLASS_TEST_CAP_BAND_ID);
        when(bandScopeRepository.findByGradeLevelIdAndFrameworkVersionId(GRADE_LEVEL_ID, FRAMEWORK_VERSION_ID))
                .thenReturn(Optional.of(scope));
        // Build the band FIRST and stub afterwards: calling band() right inside thenReturn(...) is a stub nested in a stub,
        // Mockito throws UnfinishedStubbingException.
        FrameworkResultBand centralizeCap = band("B2", 4);
        FrameworkResultBand classTestCap = band("C1", 5);
        when(frameworkResultBandRepository.findById(CENTRALIZE_CAP_BAND_ID)).thenReturn(Optional.of(centralizeCap));
        when(frameworkResultBandRepository.findById(CLASS_TEST_CAP_BAND_ID)).thenReturn(Optional.of(classTestCap));
    }

    private static FrameworkResultBand band(String label, int order) {
        FrameworkResultBand band = mock(FrameworkResultBand.class);
        when(band.getLabel()).thenReturn(label);
        when(band.getOrder()).thenReturn(order);
        return band;
    }

    private void stubClassPointingAtGrade() {
        SchoolClass schoolClass = mock(SchoolClass.class);
        when(schoolClass.getSchoolGradeId()).thenReturn(SCHOOL_GRADE_ID);
        when(schoolClassRepository.findById(SCHOOL_CLASS_ID)).thenReturn(Optional.of(schoolClass));
    }

    private void stubGradePointingAtLevel() {
        SchoolGrade schoolGrade = mock(SchoolGrade.class);
        when(schoolGrade.getGradeLevelId()).thenReturn(GRADE_LEVEL_ID);
        when(schoolGradeRepository.findById(SCHOOL_GRADE_ID)).thenReturn(Optional.of(schoolGrade));
    }

    @Test
    void rejects_whenTargetExceedsCentralizeCapAtGradeLevelScope() {
        stubCaps();

        assertThatThrownBy(() -> guard.assertWithinScope(
                GRADE_LEVEL_ID, null, null, FRAMEWORK_VERSION_ID, band("C1", 5)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Khối 10")
                .hasMessageContaining("B2");
    }

    @Test
    void rejects_whenTargetExceedsCentralizeCapViaSchoolGradeScope() {
        stubCaps();
        stubGradePointingAtLevel();

        assertThatThrownBy(() -> guard.assertWithinScope(
                null, SCHOOL_GRADE_ID, null, FRAMEWORK_VERSION_ID, band("C1", 5)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void allows_whenTargetEqualsCentralizeCap() {
        stubCaps();

        assertThatCode(() -> guard.assertWithinScope(
                GRADE_LEVEL_ID, null, null, FRAMEWORK_VERSION_ID, band("B2", 4)))
                .doesNotThrowAnyException();
    }

    @Test
    void allows_whenClassScopeReachesClassTestCap() {
        // Exactly what CLASS_TEST allows but CENTRALIZE does not: the same band order 5, blocked at the Grade/School-year
        // scope (the 2 rejects_ tests above) but let through at the Class scope.
        stubCaps();
        stubClassPointingAtGrade();
        stubGradePointingAtLevel();

        assertThatCode(() -> guard.assertWithinScope(
                null, null, SCHOOL_CLASS_ID, FRAMEWORK_VERSION_ID, band("C1", 5)))
                .doesNotThrowAnyException();
    }

    @Test
    void rejects_whenClassScopeExceedsClassTestCap() {
        // Even at the narrowest scope (Class), Band 6 is still always blocked.
        stubCaps();
        stubClassPointingAtGrade();
        stubGradePointingAtLevel();

        assertThatThrownBy(() -> guard.assertWithinScope(
                null, null, SCHOOL_CLASS_ID, FRAMEWORK_VERSION_ID, band("C2", 6)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void allows_whenGradeLevelHasNoBandScopeConfigured() {
        // Open on purpose: no ceiling declared means no blocking. See the comment in the guard.
        when(bandScopeRepository.findByGradeLevelIdAndFrameworkVersionId(GRADE_LEVEL_ID, FRAMEWORK_VERSION_ID))
                .thenReturn(Optional.empty());

        assertThatCode(() -> guard.assertWithinScope(
                GRADE_LEVEL_ID, null, null, FRAMEWORK_VERSION_ID, band("C2", 6)))
                .doesNotThrowAnyException();
    }

    @Test
    void allows_whenClassToGradeChainIsBroken() {
        // The class points to a school year that has vanished -> the grade cannot be inferred. Do not block the user over a data
        // error, the guard only logs a WARN.
        stubCaps();
        when(schoolClassRepository.findById(SCHOOL_CLASS_ID)).thenReturn(Optional.empty());

        assertThatCode(() -> guard.assertWithinScope(
                null, null, SCHOOL_CLASS_ID, FRAMEWORK_VERSION_ID, band("C1", 5)))
                .doesNotThrowAnyException();
    }

    @Test
    void defaultTargetBand_returnsCentralizeCapBand() {
        GradeLevelBandScope scope = mock(GradeLevelBandScope.class);
        when(scope.getDefaultTargetBandId()).thenReturn(CENTRALIZE_CAP_BAND_ID);
        when(bandScopeRepository.findByGradeLevelIdAndFrameworkVersionId(GRADE_LEVEL_ID, FRAMEWORK_VERSION_ID))
                .thenReturn(Optional.of(scope));
        FrameworkResultBand centralizeCap = band("B2", 4);
        when(frameworkResultBandRepository.findById(CENTRALIZE_CAP_BAND_ID)).thenReturn(Optional.of(centralizeCap));

        assertThatCode(() -> guard.defaultTargetBand(GRADE_LEVEL_ID, FRAMEWORK_VERSION_ID)
                .orElseThrow()).doesNotThrowAnyException();
    }
}
