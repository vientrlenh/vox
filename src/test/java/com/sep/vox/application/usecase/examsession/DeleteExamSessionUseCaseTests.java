package com.sep.vox.application.usecase.examsession;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sep.vox.application.port.input.usecase.examsession.DeleteExamSessionUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.dto.UserRoleInfo;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamCandidateResult;
import com.sep.vox.domain.model.exam.ExamResultAppeal;
import com.sep.vox.domain.model.exam.ExamSession;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.repository.ExamGradingAssignmentRepository;
import com.sep.vox.domain.repository.ExamResultStatusHistoryRepository;
import com.sep.vox.domain.repository.ExamCandidateResultRepository;
import com.sep.vox.domain.repository.ExamItemCriterionScoreRepository;
import com.sep.vox.domain.repository.ExamItemEvaluationRepository;
import com.sep.vox.domain.repository.ExamItemEvaluationTurnRepository;
import com.sep.vox.domain.repository.ExamItemResponseRepository;
import com.sep.vox.domain.repository.ExamItemResponseTurnRepository;
import com.sep.vox.domain.repository.ExamMemberRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.ExamResultAppealItemRepository;
import com.sep.vox.domain.repository.ExamResultAppealRepository;
import com.sep.vox.domain.repository.ExamSessionRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

/**
 * Xoá phiên thi phải dọn cả đơn phúc khảo treo trên kết quả của phiên đó.
 * Không có FK nào chặn, nên bỏ sót sẽ để lại đơn mồ côi trỏ vào kết quả đã
 * biến mất — đơn im lặng rơi khỏi mọi màn hình nhưng dòng vẫn nằm lại DB.
 */
public class DeleteExamSessionUseCaseTests {

    private ExamSessionRepository examSessionRepository;
    private ExamRepository examRepository;
    private ExamItemResponseRepository examItemResponseRepository;
    private ExamCandidateResultRepository examCandidateResultRepository;
    private ExamResultAppealRepository examResultAppealRepository;
    private ExamGradingAssignmentRepository examGradingAssignmentRepository;
    private ExamResultAppealItemRepository examResultAppealItemRepository;
    private ExamResultStatusHistoryRepository examResultStatusHistoryRepository;
    private UserContextPort userContextPort;
    private SchoolUserRepository schoolUserRepository;
    private UserRoleQueryRepository userRoleQueryRepository;
    private DeleteExamSessionUseCase useCase;

    private final UUID sessionId = UUID.randomUUID();
    private final UUID examId = UUID.randomUUID();
    private final UUID schoolId = UUID.randomUUID();
    private final UUID adminId = UUID.randomUUID();
    private final UUID resultId = UUID.randomUUID();
    private final UUID appealId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        examSessionRepository = mock(ExamSessionRepository.class);
        examRepository = mock(ExamRepository.class);
        examItemResponseRepository = mock(ExamItemResponseRepository.class);
        examCandidateResultRepository = mock(ExamCandidateResultRepository.class);
        examResultAppealRepository = mock(ExamResultAppealRepository.class);
        examGradingAssignmentRepository = mock(ExamGradingAssignmentRepository.class);
        examResultAppealItemRepository = mock(ExamResultAppealItemRepository.class);
        examResultStatusHistoryRepository = mock(ExamResultStatusHistoryRepository.class);
        userContextPort = mock(UserContextPort.class);
        schoolUserRepository = mock(SchoolUserRepository.class);
        userRoleQueryRepository = mock(UserRoleQueryRepository.class);

        useCase = new DeleteExamSessionUseCase(
            examSessionRepository,
            examRepository,
            mock(ExamMemberRepository.class),
            schoolUserRepository,
            userRoleQueryRepository,
            userContextPort,
            examItemResponseRepository,
            mock(ExamItemResponseTurnRepository.class),
            mock(ExamItemEvaluationRepository.class),
            mock(ExamItemEvaluationTurnRepository.class),
            mock(ExamItemCriterionScoreRepository.class),
            examCandidateResultRepository,
            examResultAppealRepository,
            examResultAppealItemRepository,
            examGradingAssignmentRepository,
            examResultStatusHistoryRepository
        );

        var session = new ExamSession();
        session.setId(sessionId);
        session.setExamId(examId);
        when(examSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

        var exam = new Exam();
        exam.setId(examId);
        exam.setSchoolId(schoolId);
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));

        // SCHOOL_ADMIN cùng trường với bài thi — nhánh cho phép xoá.
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(adminId);
        when(schoolUserRepository.findByUserId(adminId))
            .thenReturn(Optional.of(new SchoolUser(schoolId, adminId, null, null)));
        when(userRoleQueryRepository.findByUserIdWithRoleInfo(adminId)).thenReturn(List.of(
            new UserRoleInfo(UUID.randomUUID(), adminId, UUID.randomUUID(), null, "SCHOOL_ADMIN", "School Admin")));

        when(examItemResponseRepository.findBySessionId(sessionId)).thenReturn(List.of());
    }

    private ExamCandidateResult result() {
        var result = new ExamCandidateResult();
        result.setId(resultId);
        result.setSessionId(sessionId);
        return result;
    }

    private ExamResultAppeal appeal() {
        var appeal = new ExamResultAppeal();
        appeal.setId(appealId);
        appeal.setCandidateResultId(resultId);
        return appeal;
    }

    @Test
    void should_delete_appeal_and_its_items_before_deleting_the_result() {
        when(examCandidateResultRepository.findBySessionId(sessionId)).thenReturn(Optional.of(result()));
        when(examResultAppealRepository.findByCandidateResultId(resultId)).thenReturn(List.of(appeal()));

        useCase.execute(sessionId);

        var itemCaptor = ArgumentCaptor.forClass(java.util.Collection.class);
        verify(examResultAppealItemRepository).deleteByAppealIdIn(itemCaptor.capture());
        org.assertj.core.api.Assertions.assertThat(itemCaptor.getValue()).containsExactly(appealId);

        verify(examResultAppealRepository).deleteByIdIn(List.of(appealId));
        verify(examCandidateResultRepository).deleteBySessionId(sessionId);
        verify(examSessionRepository).deleteById(sessionId);
    }

    @Test
    void should_delete_grading_assignments_and_status_history_of_the_result() {
        when(examCandidateResultRepository.findBySessionId(sessionId)).thenReturn(Optional.of(result()));
        when(examResultAppealRepository.findByCandidateResultId(resultId)).thenReturn(List.of());

        useCase.execute(sessionId);

        // Hai bảng này treo trên candidate_result và KHÔNG có FK — bỏ sót là để lại
        // phân công và nhật ký trỏ vào một kết quả đã biến mất.
        verify(examGradingAssignmentRepository).deleteByCandidateResultIdIn(List.of(resultId));
        verify(examResultStatusHistoryRepository).deleteByCandidateResultIdIn(List.of(resultId));
    }

    @Test
    void should_not_touch_appeal_tables_when_result_has_no_appeal() {
        when(examCandidateResultRepository.findBySessionId(sessionId)).thenReturn(Optional.of(result()));
        when(examResultAppealRepository.findByCandidateResultId(resultId)).thenReturn(List.of());

        useCase.execute(sessionId);

        verify(examResultAppealItemRepository, never()).deleteByAppealIdIn(anyList());
        verify(examResultAppealRepository, never()).deleteByIdIn(any());
        verify(examSessionRepository).deleteById(sessionId);
    }

    @Test
    void should_still_delete_session_when_it_has_no_result() {
        when(examCandidateResultRepository.findBySessionId(sessionId)).thenReturn(Optional.empty());

        useCase.execute(sessionId);

        verify(examResultAppealRepository, never()).findByCandidateResultId(any());
        verify(examSessionRepository).deleteById(sessionId);
    }

    @Test
    void should_delete_appeal_items_before_their_parent_appeal() {
        when(examCandidateResultRepository.findBySessionId(sessionId)).thenReturn(Optional.of(result()));
        when(examResultAppealRepository.findByCandidateResultId(resultId)).thenReturn(List.of(appeal()));

        useCase.execute(sessionId);

        // Con trước cha: không có FK nào chặn, sai thứ tự là để lại dòng mồ côi.
        var inOrder = org.mockito.Mockito.inOrder(
            examResultAppealItemRepository, examResultAppealRepository);
        inOrder.verify(examResultAppealItemRepository).deleteByAppealIdIn(List.of(appealId));
        inOrder.verify(examResultAppealRepository).deleteByIdIn(List.of(appealId));
    }
}
