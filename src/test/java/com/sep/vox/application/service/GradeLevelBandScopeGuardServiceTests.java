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
 * Điểm mấu chốt của bộ test này: trần bậc phải giữ được KỂ CẢ khi Assessment Policy chọn phạm vi
 * hẹp hơn Khối. Policy bắt buộc chọn đúng 1 trong 3 phạm vi (Lớp | Khối năm học | Khối), nên nếu
 * chỉ canh lúc phạm vi là Khối thì chọn phạm vi Lớp là trần tự mất tác dụng.
 */
class GradeLevelBandScopeGuardServiceTests {

    private static final UUID GRADE_LEVEL_ID = UUID.randomUUID();
    private static final UUID FRAMEWORK_VERSION_ID = UUID.randomUUID();
    private static final UUID SCHOOL_GRADE_ID = UUID.randomUUID();
    private static final UUID SCHOOL_CLASS_ID = UUID.randomUUID();
    private static final UUID HARD_MAX_BAND_ID = UUID.randomUUID();
    private static final UUID DEFAULT_BAND_ID = UUID.randomUUID();

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
        var gradeLevelRepository = mock(GradeLevelRepository.class);

        GradeLevel gradeLevel = mock(GradeLevel.class);
        when(gradeLevel.getName()).thenReturn("Khối 10");
        when(gradeLevelRepository.findById(GRADE_LEVEL_ID)).thenReturn(Optional.of(gradeLevel));

        guard = new GradeLevelBandScopeGuardService(bandScopeRepository, frameworkResultBandRepository,
                gradeLevelRepository, schoolGradeRepository, schoolClassRepository);
    }

    /**
     * Trần của khối = bậc order 3. Stub cả defaultTargetBandId lẫn hardMaxBandId trỏ về CÙNG một
     * band: guard luôn resolve cả hai trước khi tính effectiveCeilingBand (xem invariant "cả hai
     * bậc phải cùng tồn tại" trong {@link GradeLevelBandScope}), nên chỉ stub hardMaxBandId khiến
     * defaultBand rỗng và guard bail-out êm thay vì ném lỗi -- không phản ánh đúng dữ liệu thật.
     */
    private void stubCapAtOrder3() {
        GradeLevelBandScope scope = mock(GradeLevelBandScope.class);
        when(scope.getHardMaxBandId()).thenReturn(HARD_MAX_BAND_ID);
        when(scope.getDefaultTargetBandId()).thenReturn(HARD_MAX_BAND_ID);
        when(bandScopeRepository.findByGradeLevelIdAndFrameworkVersionId(GRADE_LEVEL_ID, FRAMEWORK_VERSION_ID))
                .thenReturn(Optional.of(scope));
        // Dựng band TRƯỚC rồi mới stub: gọi band() ngay trong thenReturn(...) là stub lồng stub,
        // Mockito ném UnfinishedStubbingException.
        FrameworkResultBand cap = band("B1", 3);
        when(frameworkResultBandRepository.findById(HARD_MAX_BAND_ID)).thenReturn(Optional.of(cap));
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
    void rejects_whenTargetExceedsCapAtGradeLevelScope() {
        stubCapAtOrder3();

        assertThatThrownBy(() -> guard.assertWithinScope(
                GRADE_LEVEL_ID, null, null, FRAMEWORK_VERSION_ID, band("C1", 5)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Khối 10")
                .hasMessageContaining("B1");
    }

    @Test
    void rejects_whenTargetExceedsCapViaSchoolGradeScope() {
        stubCapAtOrder3();
        stubGradePointingAtLevel();

        assertThatThrownBy(() -> guard.assertWithinScope(
                null, SCHOOL_GRADE_ID, null, FRAMEWORK_VERSION_ID, band("C1", 5)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejects_whenTargetExceedsCapViaSchoolClassScope() {
        // Đường lách cũ: chọn phạm vi Lớp thì policy không mang gradeLevelId, trần không áp được.
        stubCapAtOrder3();
        stubClassPointingAtGrade();
        stubGradePointingAtLevel();

        assertThatThrownBy(() -> guard.assertWithinScope(
                null, null, SCHOOL_CLASS_ID, FRAMEWORK_VERSION_ID, band("C1", 5)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void allows_whenTargetEqualsCap() {
        stubCapAtOrder3();

        assertThatCode(() -> guard.assertWithinScope(
                GRADE_LEVEL_ID, null, null, FRAMEWORK_VERSION_ID, band("B1", 3)))
                .doesNotThrowAnyException();
    }

    @Test
    void allows_whenGradeLevelHasNoBandScopeConfigured() {
        // Mở có chủ đích: chưa khai trần thì không chặn. Xem chú thích trong guard.
        when(bandScopeRepository.findByGradeLevelIdAndFrameworkVersionId(GRADE_LEVEL_ID, FRAMEWORK_VERSION_ID))
                .thenReturn(Optional.empty());

        assertThatCode(() -> guard.assertWithinScope(
                GRADE_LEVEL_ID, null, null, FRAMEWORK_VERSION_ID, band("C2", 6)))
                .doesNotThrowAnyException();
    }

    @Test
    void allows_whenClassToGradeChainIsBroken() {
        // Lớp trỏ tới năm học đã biến mất -> không suy được khối. Không chặn người dùng vì lỗi
        // dữ liệu, guard chỉ ghi WARN.
        stubCapAtOrder3();
        when(schoolClassRepository.findById(SCHOOL_CLASS_ID)).thenReturn(Optional.empty());

        assertThatCode(() -> guard.assertWithinScope(
                null, null, SCHOOL_CLASS_ID, FRAMEWORK_VERSION_ID, band("C1", 5)))
                .doesNotThrowAnyException();
    }

    @Test
    void defaultTargetBand_returnsConfiguredBandForPrefill() {
        GradeLevelBandScope scope = mock(GradeLevelBandScope.class);
        when(scope.getDefaultTargetBandId()).thenReturn(DEFAULT_BAND_ID);
        when(bandScopeRepository.findByGradeLevelIdAndFrameworkVersionId(GRADE_LEVEL_ID, FRAMEWORK_VERSION_ID))
                .thenReturn(Optional.of(scope));
        FrameworkResultBand defaultBand = band("A2", 2);
        when(frameworkResultBandRepository.findById(DEFAULT_BAND_ID)).thenReturn(Optional.of(defaultBand));

        assertThatCode(() -> guard.defaultTargetBand(GRADE_LEVEL_ID, FRAMEWORK_VERSION_ID)
                .orElseThrow()).doesNotThrowAnyException();
    }
}
