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
 * Hai điểm mấu chốt của bộ test này:
 *
 * <p>1. Trần bậc phải giữ được KỂ CẢ khi Assessment Policy chọn phạm vi hẹp hơn Khối. Policy bắt
 * buộc chọn đúng 1 trong 3 phạm vi (Lớp | Khối năm học | Khối), nên nếu chỉ canh lúc phạm vi là
 * Khối thì chọn phạm vi Lớp là trần tự mất tác dụng.
 *
 * <p>2. Trần khác nhau theo LOẠI BÀI KIỂM TRA: CENTRALIZE (phạm vi Khối/Niên khóa, không neo vào
 * 1 Lớp cụ thể) chỉ được trần thấp; CLASS_TEST (phạm vi neo đúng 1 Lớp) được nới trần cao hơn.
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
        var gradeLevelRepository = mock(GradeLevelRepository.class);

        GradeLevel gradeLevel = mock(GradeLevel.class);
        when(gradeLevel.getName()).thenReturn("Khối 10");
        when(gradeLevelRepository.findById(GRADE_LEVEL_ID)).thenReturn(Optional.of(gradeLevel));

        guard = new GradeLevelBandScopeGuardService(bandScopeRepository, frameworkResultBandRepository,
                gradeLevelRepository, schoolGradeRepository, schoolClassRepository);
    }

    /** Trần CENTRALIZE = bậc order 4 ("B2"); trần CLASS_TEST = bậc order 5 ("C1"). */
    private void stubCaps() {
        GradeLevelBandScope scope = mock(GradeLevelBandScope.class);
        when(scope.getDefaultTargetBandId()).thenReturn(CENTRALIZE_CAP_BAND_ID);
        when(scope.getHardMaxBandId()).thenReturn(CLASS_TEST_CAP_BAND_ID);
        when(bandScopeRepository.findByGradeLevelIdAndFrameworkVersionId(GRADE_LEVEL_ID, FRAMEWORK_VERSION_ID))
                .thenReturn(Optional.of(scope));
        // Dựng band TRƯỚC rồi mới stub: gọi band() ngay trong thenReturn(...) là stub lồng stub,
        // Mockito ném UnfinishedStubbingException.
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
        // Đúng cái CLASS_TEST cho phép mà CENTRALIZE thì không: cùng bậc order 5, chặn ở scope
        // Khối/Niên khóa (2 test rejects_ ở trên) nhưng cho qua ở scope Lớp.
        stubCaps();
        stubClassPointingAtGrade();
        stubGradePointingAtLevel();

        assertThatCode(() -> guard.assertWithinScope(
                null, null, SCHOOL_CLASS_ID, FRAMEWORK_VERSION_ID, band("C1", 5)))
                .doesNotThrowAnyException();
    }

    @Test
    void rejects_whenClassScopeExceedsClassTestCap() {
        // Dù đã ở phạm vi hẹp nhất (Lớp), Bậc 6 vẫn luôn bị chặn.
        stubCaps();
        stubClassPointingAtGrade();
        stubGradePointingAtLevel();

        assertThatThrownBy(() -> guard.assertWithinScope(
                null, null, SCHOOL_CLASS_ID, FRAMEWORK_VERSION_ID, band("C2", 6)))
                .isInstanceOf(IllegalArgumentException.class);
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
