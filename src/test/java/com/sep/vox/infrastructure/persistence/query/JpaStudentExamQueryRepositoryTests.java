package com.sep.vox.infrastructure.persistence.query;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.grpc.test.autoconfigure.AutoConfigureTestGrpcTransport;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.config.ContainerTestConfig;
import com.sep.vox.infrastructure.persistence.entity.ExamCandidateJpaEntity;
import com.sep.vox.infrastructure.persistence.entity.ExamJpaEntity;
import com.sep.vox.infrastructure.persistence.entity.ExamScheduleJpaEntity;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Danh sách bài thi của học sinh, chạy trên DB thật.
 *
 * <p>Các luật ở đây trước nằm trong {@code ViewMyExamsUseCase} dưới dạng lọc/sắp trên stream, và
 * được kiểm bằng unit test với repository giả. Nay chúng nằm trong JPQL, nên chỗ kiểm cũng phải
 * chuyển xuống đây: chuỗi JPQL dựng bằng EntityManager không được compiler soi, sai tên
 * entity/field hay lệch một nhánh {@code CASE} chỉ lộ ra lúc chạy thật.
 *
 * <p>Một ca của bản cũ KHÔNG được chuyển sang: "ca thi thiếu ngày xếp cuối". Cột
 * {@code exam_schedules.start_date}/{@code end_date} là NOT NULL, nên trạng thái đó không tồn tại
 * được trong DB -- unit test cũ dựng được nó chỉ vì đã giả lập repository và đi vòng qua schema.
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureTestGrpcTransport
@Transactional
class JpaStudentExamQueryRepositoryTests extends ContainerTestConfig {

    @Autowired
    private JpaStudentExamQueryRepository repository;

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
    void should_hide_exam_in_draft_status() {
        givenExam("Kỳ thi nháp", "DRAFT", "PUBLISHED", now.plusSeconds(3600));

        assertThat(findAll().content()).isEmpty();
    }

    @Test
    void should_hide_exam_when_candidate_has_no_schedule() {
        var candidate = givenExam("Chưa xếp ca", "SCHEDULED", "PUBLISHED", now.plusSeconds(3600));
        candidate.setScheduleId(null);
        em.flush();

        assertThat(findAll().content()).isEmpty();
    }

    @Test
    void should_hide_exam_when_schedule_is_draft() {
        givenExam("Ca chưa publish", "SCHEDULED", "DRAFT", now.plusSeconds(3600));

        assertThat(findAll().content()).isEmpty();
    }

    @Test
    void should_hide_exam_when_schedule_is_moved() {
        givenExam("Ca đã dời", "SCHEDULED", "MOVED", now.plusSeconds(3600));

        assertThat(findAll().content()).isEmpty();
    }

    /** Kỳ thi bị huỷ vẫn phải hiện, nếu không học sinh cứ chờ một ca đã không còn. */
    @Test
    void should_keep_cancelled_exam() {
        givenExam("Kỳ thi đã huỷ", "CANCELLED", "CANCELLED", now.plusSeconds(3600));

        assertThat(findAll().content()).extracting(row -> row.examName())
            .containsExactly("Kỳ thi đã huỷ");
    }

    @Test
    void should_sort_exams_from_newest_to_oldest() {
        givenExam("Bài cũ", "CLOSED", "COMPLETED", now.minusSeconds(86400));
        givenExam("Bài mới", "SCHEDULED", "PUBLISHED", now.plusSeconds(86400));
        givenExam("Bài giữa", "SCHEDULED", "PUBLISHED", now.plusSeconds(3600));

        assertThat(findAll().content()).extracting(row -> row.examName())
            .containsExactly("Bài mới", "Bài giữa", "Bài cũ");
    }

    @Test
    void should_sort_ascending_when_asked() {
        givenExam("Bài cũ", "CLOSED", "COMPLETED", now.minusSeconds(86400));
        givenExam("Bài mới", "SCHEDULED", "PUBLISHED", now.plusSeconds(86400));

        var page = repository.findMyExams(studentId, null, null, false, 1, 20, now);

        assertThat(page.content()).extracting(row -> row.examName())
            .containsExactly("Bài cũ", "Bài mới");
    }

    @Test
    void should_filter_by_kind() {
        givenExam("Kỳ thi tập trung", "SCHEDULED", "PUBLISHED", now.plusSeconds(3600));
        givenExam("Bài trên lớp", "SCHEDULED", "PUBLISHED", now.plusSeconds(7200), "CLASS_TEST");

        var page = repository.findMyExams(studentId, "CLASS_TEST", null, true, 1, 20, now);

        assertThat(page.content()).extracting(row -> row.examName()).containsExactly("Bài trên lớp");
    }

    @Test
    void should_filter_by_derived_status() {
        givenExam("Sắp thi", "SCHEDULED", "PUBLISHED", now.plusSeconds(3600));
        givenExam("Đã xong", "CLOSED", "COMPLETED", now.minusSeconds(86400));

        var page = repository.findMyExams(studentId, null, "completed", true, 1, 20, now);

        assertThat(page.content()).extracting(row -> row.examName()).containsExactly("Đã xong");
    }

    /** Bài đang diễn ra: {@code now} nằm trong khoảng ca thi. */
    @Test
    void should_derive_in_progress_from_the_schedule_window() {
        givenExam("Đang thi", "IN_PROGRESS", "PUBLISHED", now.minusSeconds(600));

        assertThat(findAll().content()).extracting(row -> row.derivedStatus())
            .containsExactly("in_progress");
    }

    /**
     * Trang đầu là 1. Đây chính là chỗ lỗi cũ trú: khi repository nhân thẳng {@code page * size},
     * client xin trang 1 nhận về trang thứ hai và trang đầu không có đường nào tới.
     */
    @Test
    void should_paginate_from_page_one() {
        givenExam("Bài 1", "SCHEDULED", "PUBLISHED", now.plusSeconds(300));
        givenExam("Bài 2", "SCHEDULED", "PUBLISHED", now.plusSeconds(200));
        givenExam("Bài 3", "SCHEDULED", "PUBLISHED", now.plusSeconds(100));

        var firstPage = repository.findMyExams(studentId, null, null, true, 1, 2, now);
        var secondPage = repository.findMyExams(studentId, null, null, true, 2, 2, now);

        assertThat(firstPage.content()).extracting(row -> row.examName()).containsExactly("Bài 1", "Bài 2");
        assertThat(firstPage.page()).isEqualTo(1);
        assertThat(firstPage.totalElements()).isEqualTo(3);
        assertThat(firstPage.totalPages()).isEqualTo(2);
        assertThat(secondPage.content()).extracting(row -> row.examName()).containsExactly("Bài 3");
    }

    /** Bài thi của em khác không được lọt vào danh sách. */
    @Test
    void should_only_return_exams_of_the_given_student() {
        givenExam("Của mình", "SCHEDULED", "PUBLISHED", now.plusSeconds(3600));
        // `student_id` là updatable=false, nên phải gán ngay lúc insert: sửa sau persist thì
        // Hibernate bỏ qua và fixture âm thầm trỏ về cùng một học sinh.
        givenExamFor(UUID.randomUUID(), "Của bạn", "SCHEDULED", "PUBLISHED", now.plusSeconds(3600), "CENTRALIZED");

        assertThat(findAll().content()).extracting(row -> row.examName()).containsExactly("Của mình");
    }

    @Test
    void should_report_an_empty_page_without_running_the_row_query() {
        var page = findAll();

        assertThat(page.content()).isEmpty();
        assertThat(page.totalElements()).isZero();
        assertThat(page.totalPages()).isZero();
        assertThat(page.page()).isEqualTo(1);
    }

    private com.sep.vox.domain.common.PageResult<com.sep.vox.application.query.dto.StudentExamRowInfo> findAll() {
        return repository.findMyExams(studentId, null, null, true, 1, 20, now);
    }

    private ExamCandidateJpaEntity givenExam(
            String name, String examStatus, String scheduleStatus, Instant startDate) {
        return givenExam(name, examStatus, scheduleStatus, startDate, "CENTRALIZED");
    }

    private ExamCandidateJpaEntity givenExam(
            String name, String examStatus, String scheduleStatus, Instant startDate, String kind) {
        return givenExamFor(studentId, name, examStatus, scheduleStatus, startDate, kind);
    }

    private ExamCandidateJpaEntity givenExamFor(
            UUID owner, String name, String examStatus, String scheduleStatus, Instant startDate, String kind) {
        var exam = persisted(new ExamJpaEntity(
            null, null, null, "EX-" + UUID.randomUUID(), name, name + " mô tả", null, UUID.randomUUID(),
            kind, null, examStatus, 1, null, null, null, null,
            startDate, null, null, true,
            now, now, null, null));

        var schedule = persisted(new ExamScheduleJpaEntity(
            null, exam.getId(), null, startDate, startDate.plusSeconds(3600), scheduleStatus,
            null, now, now, null, null));

        return persisted(new ExamCandidateJpaEntity(
            null, exam.getId(), owner, null, schedule.getId(),
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
