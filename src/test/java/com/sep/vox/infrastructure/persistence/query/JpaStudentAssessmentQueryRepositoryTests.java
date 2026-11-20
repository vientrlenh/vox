package com.sep.vox.infrastructure.persistence.query;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.grpc.test.autoconfigure.AutoConfigureTestGrpcTransport;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.query.dto.StudentAssessmentRowInfo;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.infrastructure.persistence.entity.AssessmentCandidateJpaEntity;
import com.sep.vox.infrastructure.persistence.entity.AssessmentJpaEntity;
import com.sep.vox.infrastructure.persistence.entity.AssessmentScheduleJpaEntity;
import com.sep.vox.support.IntegrationTest;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * The student's assessment list, running on the real DB.
 *
 * <p>The rules here used to live in {@code ViewMyAssessmentsUseCase} as filtering/sorting on a stream, and
 * were checked by unit tests with a fake repository. Now they live in the JPQL, so the checking has to
 * move down here too: a JPQL string built with the EntityManager is not inspected by the compiler, and a wrong
 * entity/field name or one {@code CASE} branch off only shows up at real run time.
 *
 * <p>One case of the old version was NOT carried over: "a session missing its last arrangement date". The columns
 * {@code assessment_schedules.start_date}/{@code end_date} are NOT NULL, so that state cannot exist
 * in the DB -- the old unit test could build it only because it faked the repository and went around the schema.
 */
@IntegrationTest
@AutoConfigureTestGrpcTransport
@Transactional
class JpaStudentAssessmentQueryRepositoryTests {

    @Autowired
    private JpaStudentAssessmentQueryRepository repository;

    @PersistenceContext
    private EntityManager em;

    private UUID studentId;
    private Instant now;

    @BeforeEach
    void setUp() {
        studentId = UUID.randomUUID();
        now = OffsetDateTime.parse("2026-07-29T09:00:00+07:00").toInstant();
    }

    @Test
    void should_hide_assessment_in_draft_status() {
        givenAssessment("Kỳ thi nháp", "DRAFT", "PUBLISHED", now.plusSeconds(3600));

        assertThat(findAll().content()).isEmpty();
    }

    @Test
    void should_hide_assessment_when_candidate_has_no_schedule() {
        AssessmentCandidateJpaEntity candidate = givenAssessment("Chưa xếp ca", "SCHEDULED", "PUBLISHED", now.plusSeconds(3600));
        candidate.setScheduleId(null);
        em.flush();

        assertThat(findAll().content()).isEmpty();
    }

    @Test
    void should_hide_assessment_when_schedule_is_draft() {
        givenAssessment("Ca chưa publish", "SCHEDULED", "DRAFT", now.plusSeconds(3600));

        assertThat(findAll().content()).isEmpty();
    }

    @Test
    void should_hide_assessment_when_schedule_is_moved() {
        givenAssessment("Ca đã dời", "SCHEDULED", "MOVED", now.plusSeconds(3600));

        assertThat(findAll().content()).isEmpty();
    }

    /** Kỳ thi bị huỷ vẫn phải hiện, nếu không học sinh cứ chờ một ca đã không còn. */
    @Test
    void should_keep_cancelled_assessment() {
        givenAssessment("Kỳ thi đã huỷ", "CANCELLED", "CANCELLED", now.plusSeconds(3600));

        assertThat(findAll().content()).extracting(row -> row.assessmentName())
            .containsExactly("Kỳ thi đã huỷ");
    }

    @Test
    void should_sort_assessments_from_newest_to_oldest() {
        givenAssessment("Bài cũ", "CLOSED", "COMPLETED", now.minusSeconds(86400));
        givenAssessment("Bài mới", "SCHEDULED", "PUBLISHED", now.plusSeconds(86400));
        givenAssessment("Bài giữa", "SCHEDULED", "PUBLISHED", now.plusSeconds(3600));

        assertThat(findAll().content()).extracting(row -> row.assessmentName())
            .containsExactly("Bài mới", "Bài giữa", "Bài cũ");
    }

    @Test
    void should_sort_ascending_when_asked() {
        givenAssessment("Bài cũ", "CLOSED", "COMPLETED", now.minusSeconds(86400));
        givenAssessment("Bài mới", "SCHEDULED", "PUBLISHED", now.plusSeconds(86400));

        PageResult<StudentAssessmentRowInfo> page = repository.findMyAssessments(studentId, null, null, false, 1, 20, now);

        assertThat(page.content()).extracting(row -> row.assessmentName())
            .containsExactly("Bài cũ", "Bài mới");
    }

    @Test
    void should_filter_by_kind() {
        givenAssessment("Kỳ thi tập trung", "SCHEDULED", "PUBLISHED", now.plusSeconds(3600));
        givenAssessment("Bài trên lớp", "SCHEDULED", "PUBLISHED", now.plusSeconds(7200), "CLASS_TEST");

        PageResult<StudentAssessmentRowInfo> page = repository.findMyAssessments(studentId, "CLASS_TEST", null, true, 1, 20, now);

        assertThat(page.content()).extracting(row -> row.assessmentName()).containsExactly("Bài trên lớp");
    }

    @Test
    void should_filter_by_derived_status() {
        givenAssessment("Sắp thi", "SCHEDULED", "PUBLISHED", now.plusSeconds(3600));
        givenAssessment("Đã xong", "CLOSED", "COMPLETED", now.minusSeconds(86400));

        PageResult<StudentAssessmentRowInfo> page = repository.findMyAssessments(studentId, null, "completed", true, 1, 20, now);

        assertThat(page.content()).extracting(row -> row.assessmentName()).containsExactly("Đã xong");
    }

    /** Bài đang diễn ra: {@code now} nằm trong khoảng ca thi. */
    @Test
    void should_derive_in_progress_from_the_schedule_window() {
        givenAssessment("Đang thi", "IN_PROGRESS", "PUBLISHED", now.minusSeconds(600));

        assertThat(findAll().content()).extracting(row -> row.derivedStatus())
            .containsExactly("in_progress");
    }

    /**
     * The first page is 1. This is exactly where the old bug lived: when the repository multiplied
     * {@code page * size} directly, a client asking for page 1 got the second page and the first
     * page was unreachable.
     */
    @Test
    void should_paginate_from_page_one() {
        givenAssessment("Bài 1", "SCHEDULED", "PUBLISHED", now.plusSeconds(300));
        givenAssessment("Bài 2", "SCHEDULED", "PUBLISHED", now.plusSeconds(200));
        givenAssessment("Bài 3", "SCHEDULED", "PUBLISHED", now.plusSeconds(100));

        PageResult<StudentAssessmentRowInfo> firstPage = repository.findMyAssessments(studentId, null, null, true, 1, 2, now);
        PageResult<StudentAssessmentRowInfo> secondPage = repository.findMyAssessments(studentId, null, null, true, 2, 2, now);

        assertThat(firstPage.content()).extracting(row -> row.assessmentName()).containsExactly("Bài 1", "Bài 2");
        assertThat(firstPage.page()).isEqualTo(1);
        assertThat(firstPage.totalElements()).isEqualTo(3);
        assertThat(firstPage.totalPages()).isEqualTo(2);
        assertThat(secondPage.content()).extracting(row -> row.assessmentName()).containsExactly("Bài 3");
    }

    /** Bài thi của em khác không được lọt vào danh sách. */
    @Test
    void should_only_return_assessments_of_the_given_student() {
        givenAssessment("Của mình", "SCHEDULED", "PUBLISHED", now.plusSeconds(3600));
        // `student_id` là updatable=false, nên phải gán ngay lúc insert: sửa sau persist thì
        // Hibernate bỏ qua và fixture âm thầm trỏ về cùng một học sinh.
        givenAssessmentFor(UUID.randomUUID(), "Của bạn", "SCHEDULED", "PUBLISHED", now.plusSeconds(3600), "CENTRALIZED");

        assertThat(findAll().content()).extracting(row -> row.assessmentName()).containsExactly("Của mình");
    }

    @Test
    void should_report_an_empty_page_without_running_the_row_query() {
        PageResult<StudentAssessmentRowInfo> page = findAll();

        assertThat(page.content()).isEmpty();
        assertThat(page.totalElements()).isZero();
        assertThat(page.totalPages()).isZero();
        assertThat(page.page()).isEqualTo(1);
    }

    private com.sep.vox.domain.common.PageResult<com.sep.vox.application.query.dto.StudentAssessmentRowInfo> findAll() {
        return repository.findMyAssessments(studentId, null, null, true, 1, 20, now);
    }

    private AssessmentCandidateJpaEntity givenAssessment(
            String name, String assessmentStatus, String scheduleStatus, Instant startDate) {
        return givenAssessment(name, assessmentStatus, scheduleStatus, startDate, "CENTRALIZED");
    }

    private AssessmentCandidateJpaEntity givenAssessment(
            String name, String assessmentStatus, String scheduleStatus, Instant startDate, String kind) {
        return givenAssessmentFor(studentId, name, assessmentStatus, scheduleStatus, startDate, kind);
    }

    private AssessmentCandidateJpaEntity givenAssessmentFor(
            UUID owner, String name, String assessmentStatus, String scheduleStatus, Instant startDate, String kind) {
        AssessmentJpaEntity assessment = persisted(new AssessmentJpaEntity(
            null, null, null, "EX-" + UUID.randomUUID(), name, name + " mô tả", null, UUID.randomUUID(),
            kind, null, assessmentStatus, 1, null, null, null, null,
            startDate, null, null, true,
            now, now, null, null));

        AssessmentScheduleJpaEntity schedule = persisted(new AssessmentScheduleJpaEntity(
            null, assessment.getId(), null, startDate, startDate.plusSeconds(3600), scheduleStatus,
            null, now, now, null, null));

        return persisted(new AssessmentCandidateJpaEntity(
            null, assessment.getId(), owner, null, schedule.getId(),
            "ASSIGNED", now, now, null, null, null));
    }

    /**
     * Id do DB sinh ({@code @Generated(INSERT)}, {@code insertable=false}) nên fixture phải persist
     * với id null rồi đọc lại -- truyền id sẵn bị coi là detached.
     */
    private <T> T persisted(T entity) {
        em.persist(entity);
        em.flush();
        return entity;
    }
}
