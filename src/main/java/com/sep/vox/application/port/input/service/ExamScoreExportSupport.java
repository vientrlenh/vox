package com.sep.vox.application.port.input.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.GradingScopeKind;
import com.sep.vox.application.port.input.query.ExportExamScoresQuery;
import com.sep.vox.application.query.dto.ExamScoreRowInfo;
import com.sep.vox.application.query.dto.GradingAssignmentFilter;
import com.sep.vox.application.query.repository.ExamGradingQueryRepository;

/**
 * Phần dùng chung của mọi định dạng xuất bảng điểm: phân quyền, chốt phạm vi, nạp dòng.
 *
 * <p>Có hai use case xuất (CSV và Excel) và chúng chỉ khác nhau ở cách ghi file. Nếu mỗi
 * bên tự gọi phân quyền thì sớm muộn một bên được siết còn bên kia bị bỏ quên — mà bên bị
 * bỏ quên là một đường vòng để lấy nguyên bảng điểm của trường.
 */
@Service
public class ExamScoreExportSupport {

    private final ExamGradingQueryRepository examGradingQueryRepository;
    private final ExamGradingAccessService examGradingAccessService;

    public ExamScoreExportSupport(
            ExamGradingQueryRepository examGradingQueryRepository,
            ExamGradingAccessService examGradingAccessService) {
        this.examGradingQueryRepository = examGradingQueryRepository;
        this.examGradingAccessService = examGradingAccessService;
    }

    @Transactional(readOnly = true)
    public List<ExamScoreRowInfo> loadRows(ExportExamScoresQuery input) {
        var currentUserId = examGradingAccessService.requireActiveUserId();
        var schoolId = examGradingAccessService.requireCurrentSchoolId(currentUserId);
        // Giáo viên tạo bài kiểm tra trên lớp xuất được bảng điểm của ĐÚNG bài mình —
        // phạm vi đóng bằng examId, nên không rò dữ liệu kỳ thi khác của trường.
        examGradingAccessService.authorizeSchoolAdminOrClassTestChair(
            schoolId, input.examId(), currentUserId);

        // Không phạm vi = xuất mọi kỳ thi của cả trường: hàng chục nghìn dòng dựng
        // trong RAM, kèm ba query không phân trang với mệnh đề IN khổng lồ. Bắt chọn
        // phạm vi, cùng khuôn với AutoAssignGradingUseCase.
        if (input.examId() == null && input.scheduleId() == null) {
            throw new IllegalArgumentException("Phải chọn kỳ thi hoặc ca thi để xuất bảng điểm.");
        }

        return examGradingQueryRepository.findScoreRows(toFilter(input, schoolId));
    }

    /**
     * {@code schoolId} lấy từ phiên đăng nhập chứ không từ query — đây là chốt chặn duy
     * nhất giữ file xuất nằm trong trường của người gọi.
     */
    private GradingAssignmentFilter toFilter(ExportExamScoresQuery input, UUID schoolId) {
        return new GradingAssignmentFilter(
            schoolId,
            input.examId(),
            input.scheduleId(),
            input.teacherId(),
            blankToNull(input.resultStatus()),
            blankToNull(input.roundType()),
            blankToNull(input.assignmentStatus()),
            input.unassignedOnly(),
            input.overdueOnly(),
            input.hasOpenAppeal(),
            blankToNull(input.keyword()),
            GradingScopeKind.orCentralized(input.examKind()));
    }

    /**
     * Chuỗi rỗng từ query string là "không lọc", không phải "lọc theo giá trị rỗng".
     * Không quy về null thì {@code cr.status = ''} khớp đúng 0 dòng và file xuất ra trống.
     */
    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
