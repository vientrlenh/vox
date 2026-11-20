package com.sep.vox.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.service.ExamGradingAccessService;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamCandidateResult;
import com.sep.vox.domain.model.exam.ExamKind;
import com.sep.vox.domain.model.exam.ExamMemberRole;
import com.sep.vox.domain.repository.ExamCandidateResultRepository;
import com.sep.vox.domain.repository.ExamGradingAssignmentRepository;
import com.sep.vox.domain.repository.ExamMemberRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.ExamSessionRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;

/**
 * Chốt quyền của bài kiểm tra trên lớp. Sáu use case điều phối của nhà trường đều
 * hỏi đúng những hàm ở đây, nên luật chỉ tồn tại ở một chỗ.
 */
class ExamGradingAccessClassTestTests {

    private static final UUID CLASS_TEST_ID = UUID.randomUUID();
    private static final UUID CENTRALIZED_ID = UUID.randomUUID();
    private static final UUID SCHOOL_ID = UUID.randomUUID();
    private static final UUID CHAIR_ID = UUID.randomUUID();
    private static final UUID CLASS_TEST_RESULT_ID = UUID.randomUUID();
    private static final UUID CENTRALIZED_RESULT_ID = UUID.randomUUID();

    private ExamRepository examRepository;
    private ExamMemberRepository examMemberRepository;
    private ExamCandidateResultRepository examCandidateResultRepository;
    private ExamGradingAccessService service;

    @BeforeEach
    void setUp() {
        examRepository = mock(ExamRepository.class);
        examMemberRepository = mock(ExamMemberRepository.class);
        examCandidateResultRepository = mock(ExamCandidateResultRepository.class);
        service = new ExamGradingAccessService(
            mock(ExamGradingAssignmentRepository.class),
            examCandidateResultRepository,
            mock(ExamSessionRepository.class),
            examRepository,
            examMemberRepository,
            mock(UserRepository.class),
            mock(SchoolUserRepository.class),
            mock(UserRoleQueryRepository.class),
            mock(UserContextPort.class));

        when(examRepository.findById(CLASS_TEST_ID)).thenReturn(Optional.of(exam(CLASS_TEST_ID, ExamKind.CLASS_TEST)));
        when(examRepository.findById(CENTRALIZED_ID))
            .thenReturn(Optional.of(exam(CENTRALIZED_ID, ExamKind.CENTRALIZED)));
        when(examMemberRepository.existsByExamIdAndUserIdAndRole(CLASS_TEST_ID, CHAIR_ID, ExamMemberRole.CHAIR))
            .thenReturn(true);
        when(examMemberRepository.existsByExamIdAndUserIdAndRole(CENTRALIZED_ID, CHAIR_ID, ExamMemberRole.CHAIR))
            .thenReturn(true);
    }

    @Test
    void should_recognise_class_test_chair() {
        assertThat(service.isClassTestChair(CLASS_TEST_ID, CHAIR_ID)).isTrue();
    }

    @Test
    void should_reject_someone_who_is_not_chair() {
        assertThat(service.isClassTestChair(CLASS_TEST_ID, UUID.randomUUID())).isFalse();
        assertThatThrownBy(() -> service.authorizeClassTestChair(CLASS_TEST_ID, UUID.randomUUID()))
            .isInstanceOf(ForbiddenException.class);
    }

    /**
     * Ca đối chứng quan trọng: CHAIR của kỳ thi TẬP TRUNG không được hưởng quyền này.
     * Kỳ thi tập trung do nhà trường điều phối; quyền chấm đến từ dòng phân công.
     */
    @Test
    void should_not_treat_centralized_chair_as_class_test_chair() {
        assertThat(service.isClassTestChair(CENTRALIZED_ID, CHAIR_ID)).isFalse();
    }

    /**
     * Cặp đối chứng của luật trên, và là chỗ dễ trộn lẫn nhất trong service này: CÙNG một người,
     * CÙNG một kỳ thi tập trung — cửa GHI của luồng chấm đóng, cửa ĐỌC mở. Hai hàm phải trả ngược
     * nhau ở đây, nếu có ngày chúng trả giống nhau thì một trong hai luật đã bị xoá nhầm.
     */
    @Test
    void should_treat_centralized_chair_as_exam_chair_for_reads() {
        assertThat(service.isExamChair(CENTRALIZED_ID, CHAIR_ID)).isTrue();
        assertThat(service.isClassTestChair(CENTRALIZED_ID, CHAIR_ID)).isFalse();

        service.authorizeSchoolAdminOrExamChair(SCHOOL_ID, CENTRALIZED_ID, CHAIR_ID);
        assertThatThrownBy(() -> service.authorizeSchoolAdminOrClassTestChair(
                SCHOOL_ID, CENTRALIZED_ID, CHAIR_ID))
            .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void should_not_treat_a_non_member_as_exam_chair() {
        assertThat(service.isExamChair(CENTRALIZED_ID, UUID.randomUUID())).isFalse();
        assertThat(service.isExamChair(null, CHAIR_ID)).isFalse();
        assertThatThrownBy(() -> service.authorizeSchoolAdminOrExamChair(
                SCHOOL_ID, CENTRALIZED_ID, UUID.randomUUID()))
            .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void should_flag_result_of_a_class_test() {
        when(examCandidateResultRepository.findById(CLASS_TEST_RESULT_ID))
            .thenReturn(Optional.of(result(CLASS_TEST_RESULT_ID, CLASS_TEST_ID)));

        assertThat(service.isClassTestResult(CLASS_TEST_RESULT_ID)).isTrue();
        assertThatThrownBy(() -> service.rejectClassTestCoordination(CLASS_TEST_RESULT_ID))
            .isInstanceOf(ForbiddenException.class)
            .hasMessageContaining("nhà trường không phân công");
    }

    @Test
    void should_not_flag_result_of_a_centralized_exam() {
        when(examCandidateResultRepository.findById(CENTRALIZED_RESULT_ID))
            .thenReturn(Optional.of(result(CENTRALIZED_RESULT_ID, CENTRALIZED_ID)));

        assertThat(service.isClassTestResult(CENTRALIZED_RESULT_ID)).isFalse();
        service.rejectClassTestCoordination(CENTRALIZED_RESULT_ID);
    }

    /** Lô lẫn lộn: chặn CẢ LÔ, không lọc lẻ — kết quả không được phụ thuộc dữ liệu ẩn. */
    @Test
    void should_reject_a_mixed_batch_as_a_whole() {
        givenMixedBatch();

        assertThatThrownBy(() -> service.rejectClassTestCoordination(
                List.of(CLASS_TEST_RESULT_ID, CENTRALIZED_RESULT_ID)))
            .isInstanceOf(ForbiddenException.class);
    }

    /** Nhánh thu hồi hàng loạt cần biết ĐÚNG những dòng nào phải bỏ, không phải "có hay không". */
    @Test
    void should_report_exactly_which_results_belong_to_class_tests() {
        givenMixedBatch();

        assertThat(service.classTestResultIds(List.of(CLASS_TEST_RESULT_ID, CENTRALIZED_RESULT_ID)))
            .containsExactly(CLASS_TEST_RESULT_ID);
    }

    @Test
    void should_treat_an_empty_batch_as_clean() {
        assertThat(service.classTestResultIds(List.of())).isEmpty();
        assertThat(service.anyClassTestResult(null)).isFalse();
    }

    private void givenMixedBatch() {
        when(examCandidateResultRepository.findByIdIn(anyCollection())).thenReturn(List.of(
            result(CLASS_TEST_RESULT_ID, CLASS_TEST_ID),
            result(CENTRALIZED_RESULT_ID, CENTRALIZED_ID)));
        when(examRepository.findByIdIn(anyCollection())).thenReturn(List.of(
            exam(CLASS_TEST_ID, ExamKind.CLASS_TEST),
            exam(CENTRALIZED_ID, ExamKind.CENTRALIZED)));
    }

    private Exam exam(UUID id, ExamKind kind) {
        var exam = new Exam();
        exam.setId(id);
        exam.setKind(kind);
        return exam;
    }

    private ExamCandidateResult result(UUID id, UUID examId) {
        var result = new ExamCandidateResult();
        result.setId(id);
        result.setExamId(examId);
        return result;
    }
}
