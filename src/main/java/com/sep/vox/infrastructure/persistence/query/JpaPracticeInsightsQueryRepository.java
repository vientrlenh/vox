package com.sep.vox.infrastructure.persistence.query;

import static com.sep.vox.application.response.input.practiceinsights.PracticeInsights.ClassPracticeOverview;
import static com.sep.vox.application.response.input.practiceinsights.PracticeInsights.ClassPracticeRow;
import static com.sep.vox.application.response.input.practiceinsights.PracticeInsights.CriterionProgressPoint;
import static com.sep.vox.application.response.input.practiceinsights.PracticeInsights.CriterionWeakness;
import static com.sep.vox.application.response.input.practiceinsights.PracticeInsights.SubAttributeWeakness;
import static com.sep.vox.application.response.input.practiceinsights.PracticeInsights.WeaknessExample;
import static com.sep.vox.application.response.input.practiceinsights.PracticeInsights.WeaknessProfile;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.service.WeaknessVectorSettings;
import com.sep.vox.application.query.dto.WeaknessEvidenceInfo;
import com.sep.vox.application.query.repository.PracticeInsightsQueryRepository;
import com.sep.vox.infrastructure.persistence.repository.SpringDataPracticeInsightsQueryRepository;

@Service
@Transactional(readOnly = true)
public class JpaPracticeInsightsQueryRepository
        implements PracticeInsightsQueryRepository {

    private final SpringDataPracticeInsightsQueryRepository practiceInsightsQueryRepository;
    private final WeaknessVectorSettings weaknessVectorSettings;

    /**
     * Dưới ngưỡng này ở cửa sổ TRƯỚC thì không hiện xu hướng. Học sinh luyện thưa mà chỉ
     * có 1-2 lần làm nền thì thêm một lần nữa đã thành ±100% -- con số đúng về số học
     * nhưng vô nghĩa về sư phạm.
     */
    private static final int MINIMUM_PRIOR_OCCURRENCES_FOR_TREND = 3;

    /**
     * Số ví dụ hiện cho mỗi nhãn. Ba là đủ để thấy quy luật ("lần nào cũng quên -s") mà không
     * biến hồ sơ thành một trang nhật ký lỗi.
     */
    private static final int EVIDENCE_EXAMPLES_PER_LABEL = 3;

    /**
     * Khoá gộp bằng chứng theo (tiêu chí, nhãn). Cả mã tiêu chí lẫn nhãn đều là định danh
     * không dấu cách, không chứa ký tự này, nên ghép chuỗi là an toàn.
     */
    private static final String LABEL_KEY_SEPARATOR = "/";

    /**
     * Số nhãn tối đa hiện cho mỗi tiêu chí.
     *
     * Phát âm sinh một nhãn cho MỖI âm vị; một buổi đã ra hơn chục nhãn. Không chặn thì hồ sơ
     * điểm yếu dài vô hạn theo thời gian luyện, và thứ đáng sửa nhất bị chôn giữa danh sách.
     */
    private static final int MAX_LABELS_PER_CRITERION = 5;

    public JpaPracticeInsightsQueryRepository(
            SpringDataPracticeInsightsQueryRepository practiceInsightsQueryRepository,
            WeaknessVectorSettings weaknessVectorSettings) {
        this.practiceInsightsQueryRepository = practiceInsightsQueryRepository;
        this.weaknessVectorSettings = weaknessVectorSettings;
    }

    @Override
    public WeaknessProfile weaknessProfile(UUID studentId) {
        var criteria = practiceInsightsQueryRepository.findCriterionWeaknesses(studentId).stream()
            .map(row -> new CriterionWeakness(
                row.getCriterionCode(),
                row.getCriterionName(),
                row.getWeakness(),
                row.getObservationCount(),
                row.getReliable()
            ))
            .toList();
        // Cùng cửa sổ mà WeaknessVectorCalculator dùng để tính freq/recent_freq -- lấy
        // nguồn khác là xu hướng nói một đằng còn số lần nói một nẻo.
        var windowDays = (int) weaknessVectorSettings.observationWindow().toDays();
        var recentDays = (int) weaknessVectorSettings.recentObservationWindow().toDays();
        // Bằng chứng gom SẴN thành map trước vòng lặp, một truy vấn cho cả hồ sơ -- không
        // hỏi lại DB cho từng nhãn (13 nhãn = 13 vòng gọi cho một màn hình chỉ để đọc).
        Map<String, List<WeaknessExample>> evidenceByLabel = practiceInsightsQueryRepository.findRecentEvidence(
                studentId,
                Instant.now().minus(weaknessVectorSettings.observationWindow()),
                EVIDENCE_EXAMPLES_PER_LABEL
            ).stream()
            .collect(Collectors.groupingBy(
                row -> row.getCriterionCode() + LABEL_KEY_SEPARATOR + row.getSubAttribute(),
                LinkedHashMap::new,
                Collectors.mapping(
                    row -> new WeaknessExample(
                        row.getEvidenceSpan(),
                        row.getTimes() == null ? 1 : row.getTimes()
                    ),
                    Collectors.toList()
                )
            ));
        var subAttributes = practiceInsightsQueryRepository.findSubAttributeWeaknesses(
                studentId,
                windowDays,
                recentDays,
                MINIMUM_PRIOR_OCCURRENCES_FOR_TREND,
                MAX_LABELS_PER_CRITERION
            ).stream()
            .map(row -> new SubAttributeWeakness(
                row.getCriterionCode(),
                row.getSubAttribute(),
                row.getOccurrenceCount(),
                row.getSeverity(),
                row.getPracticeable(),
                row.getTrendPercent(),
                Boolean.TRUE.equals(row.getNearlyFixed()),
                Boolean.TRUE.equals(row.getNewlyFound()),
                evidenceByLabel.getOrDefault(
                    row.getCriterionCode() + LABEL_KEY_SEPARATOR + row.getSubAttribute(), List.of()
                )
            ))
            .toList();
        var sessionsAnalysed = practiceInsightsQueryRepository.countSessionsAnalysed(
            studentId,
            Instant.now().minus(weaknessVectorSettings.observationWindow())
        );
        // Đếm TRÊN CHÍNH danh sách sẽ hiển thị, không đếm bằng một truy vấn riêng.
        //
        // Truy vấn đếm riêng (findWeaknessTrendCounts) quét toàn bộ sub_attribute_priority,
        // trong khi danh sách nhãn đã bị chặn MAX_LABELS_PER_CRITERION. Hai nguồn lệch nhau
        // sinh ra thứ vô lý ngay trên màn hình: "10 điểm yếu đang theo" mà "11 mới phát hiện".
        // Đếm từ cùng một tập thì hai con số không thể mâu thuẫn nữa, bất kể sau này đổi cách
        // chặn thế nào.
        var nearlyFixed = (int) subAttributes.stream().filter(SubAttributeWeakness::nearlyFixed).count();
        var newlyFound = (int) subAttributes.stream().filter(SubAttributeWeakness::newlyFound).count();
        return new WeaknessProfile(
            criteria,
            subAttributes,
            sessionsAnalysed,
            nearlyFixed,
            newlyFound
        );
    }

    @Override
    public List<CriterionProgressPoint> progress(UUID studentId, String criterionCode, int days) {
        var safeDays = Math.max(1, Math.min(days, 3650));
        return practiceInsightsQueryRepository.findProgress(
            studentId,
            Instant.now().minus(Duration.ofDays(safeDays)),
            criterionCode
        ).stream()
            .map(row -> new CriterionProgressPoint(
                row.getCriterionCode(),
                row.getObservedDate(),
                row.getLatentLevel(),
                row.getSource()
            ))
            .toList();
    }

    @Override
    public void requireTeacherCanReadStudent(UUID teacherId, UUID studentId) {
        if (!practiceInsightsQueryRepository.canTeacherReadStudent(teacherId, studentId)) {
            throw new ForbiddenException("Bạn không có quyền xem dữ liệu của học sinh này");
        }
    }

    @Override
    public void requireTeacherCanReadClass(UUID teacherId, UUID classId) {
        if (!practiceInsightsQueryRepository.canTeacherReadClass(teacherId, classId)) {
            throw new ForbiddenException("Bạn không có quyền xem dữ liệu của lớp này");
        }
    }

    @Override
    public ClassPracticeOverview classOverview(UUID classId) {
        var rows = practiceInsightsQueryRepository.findClassOverviewRows(classId).stream()
            .map(row -> new ClassPracticeRow(
                row.getStudentId(),
                row.getFullName(),
                0,
                0,
                null,
                null,
                row.getWeakestCriterionCode()
            ))
            .toList();
        return new ClassPracticeOverview(classId, rows.size(), 0, rows);
    }
}
