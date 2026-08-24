package com.sep.vox.application.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.query.ViewExamDetailsQuery;
import com.sep.vox.application.port.input.usecase.exam.ViewExamDetailsUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamKind;
import com.sep.vox.domain.model.exam.ExamStatus;
import com.sep.vox.domain.repository.ExamMemberRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

/**
 * Chốt một CỬA ĐÓNG: giám thị được phân công ca thi vẫn KHÔNG đọc được chi tiết kỳ thi.
 *
 * <p>Đây là quyết định chứ không phải thiếu sót, nên nó cần test giữ. {@code exam(id)} là cửa vào
 * màn quản lý kỳ thi (hội đồng và nhà trường); từng có lúc nhánh giám thị được thêm vào đây để màn
 * giám sát chạy được, và hệ quả là giám thị vào thẳng được dashboard kỳ thi. Nhu cầu thật -- giám
 * thị đọc được tên kỳ thi mình gác -- đã tách sang {@code ViewMonitorableExamUseCase}, nên nếu
 * nhánh đó quay lại đây thì là hồi quy.
 */
class ViewExamDetailsProctorAccessTests {

    private static final UUID EXAM_ID = UUID.randomUUID();
    private static final UUID SCHOOL_ID = UUID.randomUUID();
    private static final UUID PROCTOR_ID = UUID.randomUUID();

    private ExamRepository examRepository;
    private ExamMemberRepository examMemberRepository;
    private UserContextPort userContextPort;
    private SchoolUserRepository schoolUserRepository;
    private UserRoleQueryRepository userRoleQueryRepository;
    private ViewExamDetailsUseCase useCase;

    @BeforeEach
    void setUp() {
        examRepository = mock(ExamRepository.class);
        examMemberRepository = mock(ExamMemberRepository.class);
        userContextPort = mock(UserContextPort.class);
        schoolUserRepository = mock(SchoolUserRepository.class);
        userRoleQueryRepository = mock(UserRoleQueryRepository.class);

        useCase = new ViewExamDetailsUseCase(
            examRepository,
            examMemberRepository,
            userContextPort,
            schoolUserRepository,
            userRoleQueryRepository
        );

        var exam = new Exam();
        exam.setId(EXAM_ID);
        exam.setSchoolId(SCHOOL_ID);
        // Đang diễn ra: nhánh CLOSED/RESULTS_PUBLISHED chưa mở, nên kết quả nói đúng về nhánh đang xét.
        exam.setStatus(ExamStatus.IN_PROGRESS);
        // ExamDtoMapper đọc thẳng .getKind().name(); thiếu là NPE, tức test hỏng chứ không phải
        // khẳng định sai.
        exam.setKind(ExamKind.CENTRALIZED);
        when(examRepository.findById(EXAM_ID)).thenReturn(Optional.of(exam));

        when(examMemberRepository.existsByExamIdAndUserIdAndRole(any(), any(), any())).thenReturn(false);
        when(userContextPort.isSystemAdmin()).thenReturn(false);
        when(schoolUserRepository.findByUserId(any())).thenReturn(Optional.empty());
        when(userRoleQueryRepository.findByUserIdWithRoleInfo(any())).thenReturn(List.of());
    }

    @Test
    void a_proctor_who_is_not_an_exam_member_cannot_open_the_exam_dashboard() {
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(PROCTOR_ID);

        assertThatThrownBy(() -> useCase.execute(new ViewExamDetailsQuery(EXAM_ID)))
            .isInstanceOf(ForbiddenException.class);
    }
}
