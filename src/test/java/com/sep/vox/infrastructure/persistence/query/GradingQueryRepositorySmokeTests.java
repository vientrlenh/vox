package com.sep.vox.infrastructure.persistence.query;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.grpc.test.autoconfigure.AutoConfigureTestGrpcTransport;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.query.dto.GradingAssignmentFilter;
import com.sep.vox.application.query.repository.ExamGradingQueryRepository;
import com.sep.vox.application.query.repository.ExamResultAuditQueryRepository;
import com.sep.vox.config.ContainerTestConfig;

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
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureTestGrpcTransport
@Transactional
class GradingQueryRepositorySmokeTests extends ContainerTestConfig {

    @Autowired
    private ExamGradingQueryRepository examGradingQueryRepository;

    @Autowired
    private ExamResultAuditQueryRepository examResultAuditQueryRepository;

    private final UUID schoolId = UUID.randomUUID();
    private final UUID examId = UUID.randomUUID();
    private final UUID scheduleId = UUID.randomUUID();
    private final UUID someId = UUID.randomUUID();

    @Test
    void should_run_the_bulk_finalize_preview() {
        var preview = examGradingQueryRepository.previewBulkFinalize(schoolId, examId);

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
        var filter = new GradingAssignmentFilter(
            schoolId, null, null, null, null, null, null, false, false, null, null, null);

        var page = examGradingQueryRepository.searchAssignments(filter, 0, 20);

        assertThat(page.content()).isEmpty();
    }

    @Test
    void should_run_the_coordination_board_query_with_every_filter_on() {
        var filter = new GradingAssignmentFilter(
            schoolId, examId, scheduleId, someId, "PENDING_REVIEW", "APPEAL", "ASSIGNED",
            true, true, Boolean.TRUE, "nguyen", "CENTRALIZED");

        assertThat(examGradingQueryRepository.searchAssignments(filter, 0, 20).content()).isEmpty();
    }

    @Test
    void should_run_the_stats_query() {
        var stats = examGradingQueryRepository.stats(schoolId, examId, scheduleId, "CENTRALIZED");

        assertThat(stats.total()).isZero();
        assertThat(stats.unassigned()).isZero();
        assertThat(stats.assigned()).isZero();
    }

    @Test
    void should_run_the_teacher_queue_query() {
        assertThat(examGradingQueryRepository
            .findTasksByTeacherId(someId, "CENTRALIZED", null, null, 0, 20).content())
            .isEmpty();
    }

    /** Cả hai nhánh của {@code (:examKind IS NULL OR …)} — null là đường đi khác hẳn. */
    @Test
    void should_run_the_teacher_exam_picker_query() {
        assertThat(examGradingQueryRepository
            .findExamsWithTasksByTeacherId(someId, "CENTRALIZED")).isEmpty();
        assertThat(examGradingQueryRepository
            .findExamsWithTasksByTeacherId(someId, null)).isEmpty();
    }

    @Test
    void should_run_the_task_detail_query() {
        assertThat(examGradingQueryRepository.findTaskDetail(someId, someId)).isEmpty();
    }

    @Test
    void should_run_the_assignable_teacher_queries() {
        assertThat(examGradingQueryRepository.findAssignableTeachers(schoolId, "nguyen")).isEmpty();
        assertThat(examGradingQueryRepository.findTeacherIdsInSchool(schoolId, List.of(someId))).isEmpty();
        assertThat(examGradingQueryRepository.assignedLoadByTeacherIds(List.of(someId))).isEmpty();
    }

    @Test
    void should_run_the_assignable_result_query() {
        assertThat(examGradingQueryRepository.findAssignableResultIds(
            schoolId, examId, scheduleId, List.of("PENDING_REVIEW", "RELEASED"))).isEmpty();
    }

    @Test
    void should_run_the_risk_and_conflict_queries() {
        assertThat(examGradingQueryRepository.findRiskInfos(List.of(someId))).isEmpty();
        assertThat(examGradingQueryRepository.findTeacherIdsWithHumanEvaluation(someId)).isEmpty();
    }

    /**
     * Câu xuất bảng điểm nay dùng chung WHERE_CLAUSE với bảng điều phối, nên phải chạy
     * được với MỌI vị ngữ trong đó — kể cả các EXISTS lồng và tham số Boolean nullable.
     */
    @Test
    void should_run_the_score_export_query() {
        assertThat(examGradingQueryRepository.findScoreRows(
            scoreExportFilter(examId, null, "CENTRALIZED"))).isEmpty();
        assertThat(examGradingQueryRepository.findScoreRows(
            scoreExportFilter(null, scheduleId, "CLASS_TEST"))).isEmpty();
        assertThat(examGradingQueryRepository.findScoreRows(new GradingAssignmentFilter(
            schoolId, examId, null, someId, "RELEASED", "INITIAL", "COMPLETED",
            true, true, Boolean.TRUE, "nguyen", "CENTRALIZED"))).isEmpty();
    }

    private GradingAssignmentFilter scoreExportFilter(UUID examId, UUID scheduleId, String kind) {
        return new GradingAssignmentFilter(schoolId, examId, scheduleId, null, null, null, null,
            false, false, null, null, kind);
    }

    @Test
    void should_run_the_audit_queries() {
        assertThat(examResultAuditQueryRepository.findHistory(someId)).isEmpty();

        var report = examResultAuditQueryRepository.aiQualityReport(schoolId, examId);

        assertThat(report.reviewed()).isZero();
        assertThat(report.byTeacher()).isEmpty();
    }
}
