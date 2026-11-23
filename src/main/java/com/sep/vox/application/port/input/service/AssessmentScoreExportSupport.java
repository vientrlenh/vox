package com.sep.vox.application.port.input.service;

import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.query.assessmentgrading.ExportAssessmentScoresQuery;
import com.sep.vox.application.query.dto.AssessmentScoreRowInfo;
import com.sep.vox.application.query.dto.GradingAssignmentFilter;
import com.sep.vox.application.query.repository.AssessmentGradingQueryRepository;
import com.sep.vox.domain.service.assessment.GradingScopeKind;

/**
 * Phần dùng chung của mọi định dạng xuất bảng điểm: phân quyền, chốt phạm vi, nạp dòng.
 *
 * <p>Có hai use case xuất (CSV và Excel) và chúng chỉ khác nhau ở cách ghi file. Nếu mỗi
 * bên tự gọi phân quyền thì sớm muộn một bên được siết còn bên kia bị bỏ quên — mà bên bị
 * bỏ quên là một đường vòng để lấy nguyên bảng điểm của trường.
 */
@Service
@RequiredArgsConstructor
public class AssessmentScoreExportSupport {

    private final AssessmentGradingQueryRepository assessmentGradingQueryRepository;
    private final AssessmentGradingAccessService assessmentGradingAccessService;

    @Transactional(readOnly = true)
    public List<AssessmentScoreRowInfo> loadRows(ExportAssessmentScoresQuery input) {
        UUID currentUserId = assessmentGradingAccessService.requireActiveUserId();
        UUID schoolId = assessmentGradingAccessService.requireCurrentSchoolId(currentUserId);
        // The teacher who created a class test can export the score sheet of EXACTLY their own test --
        // the scope is closed by assessmentId, so no data of another assessment of the school leaks.
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
            nameOrNull(input.resultStatus()),
            nameOrNull(input.roundType()),
            nameOrNull(input.assignmentStatus()),
            input.unassignedOnly(),
            input.overdueOnly(),
            input.hasOpenAppeal(),
            blankToNull(input.keyword()),
            GradingScopeKind.orCentralized(nameOrNull(input.assessmentKind())));
    }

    /**
     * Chuỗi rỗng từ query string là "không lọc", không phải "lọc theo giá trị rỗng".
     * Không quy về null thì {@code cr.status = ''} khớp đúng 0 dòng và file xuất ra trống.
     */
    private String nameOrNull(Enum<?> value) {
        return value == null ? null : value.name();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
