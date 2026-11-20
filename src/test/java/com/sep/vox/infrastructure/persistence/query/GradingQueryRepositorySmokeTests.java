package com.sep.vox.infrastructure.persistence.query;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.grpc.test.autoconfigure.AutoConfigureTestGrpcTransport;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.query.dto.AiQualityReportInfo;
import com.sep.vox.application.query.dto.BulkFinalizePreviewInfo;
import com.sep.vox.application.query.dto.GradingAssignmentFilter;
import com.sep.vox.application.query.dto.GradingAssignmentRowInfo;
import com.sep.vox.application.query.dto.GradingStatsInfo;
import com.sep.vox.application.query.repository.AssessmentGradingQueryRepository;
import com.sep.vox.application.query.repository.AssessmentResultAuditQueryRepository;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.support.IntegrationTest;

/**
 * Mọi câu ở hai repository này dựng bằng {@code em.createQuery} — tức chỉ được phân
 * tích cú pháp lúc GỌI, không phải lúc khởi động như {@code @Query} của Spring Data.
 * Một câu JPQL sai ngoặc vì thế lọt qua cả compile lẫn context load và chỉ nổ khi người
 * dùng bấm nút. Đúng chuyện đã xảy ra với {@code previewBulkFinalize}: dấu ngoặc của
 * {@code EXISTS (} không được đóng, và không test nào chạm tới nên nó im lặng nằm đó.
 *
 * <p>Lớp này chạy từng method một lần với phạm vi rỗng. Không kiểm nghiệp vụ — cái đó
 * là việc của test use case — chỉ kiểm "câu này chạy được trên Postgres thật".
 */
@IntegrationTest
@AutoConfigureTestGrpcTransport
@Transactional
class GradingQueryRepositorySmokeTests {

    @Autowired
    private AssessmentGradingQueryRepository assessmentGradingQueryRepository;

    @Autowired
    private AssessmentResultAuditQueryRepository assessmentResultAuditQueryRepository;

    private final UUID schoolId = UUID.randomUUID();
    private final UUID assessmentId = UUID.randomUUID();
    private final UUID scheduleId = UUID.randomUUID();
    private final UUID someId = UUID.randomUUID();

    @Test
    void should_run_the_bulk_finalize_preview() {
        BulkFinalizePreviewInfo preview = assessmentGradingQueryRepository.previewBulkFinalize(schoolId, assessmentId);

        assertThat(preview.total()).isZero();
        assertThat(preview.readyToFinalize()).isZero();
        assertThat(preview.pendingUnassigned()).isZero();
        assertThat(preview.pendingAssigned()).isZero();
        assertThat(preview.openAppeals()).isZero();
        assertThat(preview.invalid()).isZero();
        assertThat(preview.blockingResultIds()).isEmpty();
        assertThat(preview.isClean()).isTrue();
    }

    @Test
    void should_run_the_coordination_board_query() {
        GradingAssignmentFilter filter = new GradingAssignmentFilter(
            schoolId, null, null, null, null, null, null, false, false, null, null, null);

        PageResult<GradingAssignmentRowInfo> page = assessmentGradingQueryRepository.searchAssignments(filter, 1, 20);

        assertThat(page.content()).isEmpty();
    }

    @Test
    void should_run_the_coordination_board_query_with_every_filter_on() {
        GradingAssignmentFilter filter = new GradingAssignmentFilter(
            schoolId, assessmentId, scheduleId, someId, "PENDING_REVIEW", "APPEAL", "ASSIGNED",
            true, true, Boolean.TRUE, "nguyen", "CENTRALIZED");

        assertThat(assessmentGradingQueryRepository.searchAssignments(filter, 1, 20).content()).isEmpty();
    }

    @Test
    void should_run_the_stats_query() {
        GradingStatsInfo stats = assessmentGradingQueryRepository.stats(schoolId, assessmentId, scheduleId, "CENTRALIZED");

        assertThat(stats.total()).isZero();
        assertThat(stats.unassigned()).isZero();
        assertThat(stats.assigned()).isZero();
    }

    @Test
    void should_run_the_teacher_queue_query() {
        assertThat(assessmentGradingQueryRepository
            .findTasksByTeacherId(someId, "CENTRALIZED", null, null, 1, 20).content())
            .isEmpty();
    }

    /** Both branches of {@code (:assessmentKind IS NULL OR …)} -- null is a completely different path. */
    @Test
    void should_run_the_teacher_assessment_picker_query() {
        assertThat(assessmentGradingQueryRepository
            .findAssessmentsWithTasksByTeacherId(someId, "CENTRALIZED")).isEmpty();
        assertThat(assessmentGradingQueryRepository
            .findAssessmentsWithTasksByTeacherId(someId, null)).isEmpty();
    }

    @Test
    void should_run_the_task_detail_query() {
        assertThat(assessmentGradingQueryRepository.findTaskDetail(someId, someId)).isEmpty();
    }

    @Test
    void should_run_the_assignable_teacher_queries() {
        assertThat(assessmentGradingQueryRepository.findAssignableTeachers(schoolId, "nguyen")).isEmpty();
        assertThat(assessmentGradingQueryRepository.findTeacherIdsInSchool(schoolId, List.of(someId))).isEmpty();
        assertThat(assessmentGradingQueryRepository.assignedLoadByTeacherIds(List.of(someId))).isEmpty();
    }

    @Test
    void should_run_the_assignable_result_query() {
        assertThat(assessmentGradingQueryRepository.findAssignableResultIds(
            schoolId, assessmentId, scheduleId, List.of("PENDING_REVIEW", "RELEASED"))).isEmpty();
    }

    @Test
    void should_run_the_risk_and_conflict_queries() {
        assertThat(assessmentGradingQueryRepository.findRiskInfos(List.of(someId))).isEmpty();
        assertThat(assessmentGradingQueryRepository.findTeacherIdsWithHumanEvaluation(someId)).isEmpty();
    }

    /**
     * Câu xuất bảng điểm nay dùng chung WHERE_CLAUSE với bảng điều phối, nên phải chạy
     * được với MỌI vị ngữ trong đó — kể cả các EXISTS lồng và tham số Boolean nullable.
     */
    @Test
    void should_run_the_score_export_query() {
        assertThat(assessmentGradingQueryRepository.findScoreRows(
            scoreExportFilter(assessmentId, null, "CENTRALIZED"))).isEmpty();
        assertThat(assessmentGradingQueryRepository.findScoreRows(
            scoreExportFilter(null, scheduleId, "CLASS_TEST"))).isEmpty();
        assertThat(assessmentGradingQueryRepository.findScoreRows(new GradingAssignmentFilter(
            schoolId, assessmentId, null, someId, "RELEASED", "INITIAL", "COMPLETED",
            true, true, Boolean.TRUE, "nguyen", "CENTRALIZED"))).isEmpty();
    }

    private GradingAssignmentFilter scoreExportFilter(UUID assessmentId, UUID scheduleId, String kind) {
        return new GradingAssignmentFilter(schoolId, assessmentId, scheduleId, null, null, null, null,
            false, false, null, null, kind);
    }

    @Test
    void should_run_the_audit_queries() {
        assertThat(assessmentResultAuditQueryRepository.findHistory(someId)).isEmpty();

        AiQualityReportInfo report = assessmentResultAuditQueryRepository.aiQualityReport(schoolId, assessmentId);

        assertThat(report.reviewed()).isZero();
        assertThat(report.byTeacher()).isEmpty();
    }
}
