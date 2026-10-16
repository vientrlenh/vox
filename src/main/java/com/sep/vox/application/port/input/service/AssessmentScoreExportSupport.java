package com.sep.vox.application.port.input.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.query.ExportAssessmentScoresQuery;
import com.sep.vox.application.projection.dto.AssessmentScoreRowInfo;
import com.sep.vox.application.projection.dto.GradingAssignmentFilter;
import com.sep.vox.application.projection.repository.AssessmentGradingQueryRepository;
import com.sep.vox.domain.service.assessment.GradingScopeKind;

/**
 * Phần dùng chung của mọi định dạng xuất bảng điểm: phân quyền, chốt phạm vi, nạp dòng.
 *
 * <p>Có hai use case xuất (CSV và Excel) và chúng chỉ khác nhau ở cách ghi file. Nếu mỗi
 * bên tự gọi phân quyền thì sớm muộn một bên được siết còn bên kia bị bỏ quên — mà bên bị
 * bỏ quên là một đường vòng để lấy nguyên bảng điểm của trường.
 */
@Service
public class AssessmentScoreExportSupport {

    private final AssessmentGradingQueryRepository assessmentGradingQueryRepository;
    private final AssessmentGradingAccessService assessmentGradingAccessService;

    public AssessmentScoreExportSupport(
            AssessmentGradingQueryRepository assessmentGradingQueryRepository,
            AssessmentGradingAccessService assessmentGradingAccessService) {
        this.assessmentGradingQueryRepository = assessmentGradingQueryRepository;
        this.assessmentGradingAccessService = assessmentGradingAccessService;
    }

    @Transactional(readOnly = true)
    public List<AssessmentScoreRowInfo> loadRows(ExportAssessmentScoresQuery input) {
        var currentUserId = assessmentGradingAccessService.requireActiveUserId();
        var schoolId = assessmentGradingAccessService.requireCurrentSchoolId(currentUserId);
        // Giáo viên tạo bài kiểm tra trên lớp xuất được bảng điểm của ĐÚNG bài mình —
        // phạm vi đóng bằng assessmentId, nên không rò dữ liệu kỳ thi khác của trường.
        assessmentGradingAccessService.authorizeSchoolAdminOrClassTestChair(
            schoolId, input.assessmentId(), currentUserId);

        // Không phạm vi = xuất mọi kỳ thi của cả trường: hàng chục nghìn dòng dựng
        // trong RAM, kèm ba query không phân trang với mệnh đề IN khổng lồ. Bắt chọn
        // phạm vi, cùng khuôn với AutoAssignGradingUseCase.
        if (input.assessmentId() == null && input.scheduleId() == null) {
            throw new IllegalArgumentException("Phải chọn kỳ thi hoặc ca thi để xuất bảng điểm.");
        }

        return assessmentGradingQueryRepository.findScoreRows(toFilter(input, schoolId));
    }

    /**
     * {@code schoolId} lấy từ phiên đăng nhập chứ không từ query — đây là chốt chặn duy
     * nhất giữ file xuất nằm trong trường của người gọi.
     */
    private GradingAssignmentFilter toFilter(ExportAssessmentScoresQuery input, UUID schoolId) {
        return new GradingAssignmentFilter(
            schoolId,
            input.assessmentId(),
            input.scheduleId(),
            input.teacherId(),
            blankToNull(input.resultStatus()),
            blankToNull(input.roundType()),
            blankToNull(input.assignmentStatus()),
            input.unassignedOnly(),
            input.overdueOnly(),
            input.hasOpenAppeal(),
            blankToNull(input.keyword()),
            GradingScopeKind.orCentralized(input.assessmentKind()));
    }

    /**
     * Chuỗi rỗng từ query string là "không lọc", không phải "lọc theo giá trị rỗng".
     * Không quy về null thì {@code cr.status = ''} khớp đúng 0 dòng và file xuất ra trống.
     */
    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
