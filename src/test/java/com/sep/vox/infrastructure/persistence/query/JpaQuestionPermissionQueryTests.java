package com.sep.vox.infrastructure.persistence.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.config.TestContainerConfig;
import com.sep.vox.domain.model.question.Question;
import com.sep.vox.domain.model.question.QuestionBank;
import com.sep.vox.domain.model.question.QuestionBankOwnerType;
import com.sep.vox.domain.model.question.QuestionBankStatus;
import com.sep.vox.domain.model.question.QuestionScope;
import com.sep.vox.domain.model.question.QuestionStatus;
import com.sep.vox.domain.model.question.QuestionTopic;
import com.sep.vox.domain.model.question.QuestionTopicStatus;
import com.sep.vox.domain.model.question.QuestionType;
import com.sep.vox.domain.model.question.QuestionVisibility;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.QuestionBankRepository;
import com.sep.vox.domain.repository.QuestionRepository;
import com.sep.vox.domain.repository.QuestionTopicRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;
import com.sep.vox.domain.valueobject.DateOfBirth;
import com.sep.vox.domain.valueobject.Email;
import com.sep.vox.domain.valueobject.FullName;
import com.sep.vox.domain.valueobject.Phone;
import com.sep.vox.infrastructure.persistence.adapter.QuestionBankRepositoryImpl;
import com.sep.vox.infrastructure.persistence.adapter.QuestionRepositoryImpl;
import com.sep.vox.infrastructure.persistence.adapter.QuestionTopicRepositoryImpl;
import com.sep.vox.infrastructure.persistence.adapter.SchoolUserRepositoryImpl;
import com.sep.vox.infrastructure.persistence.adapter.UserRepositoryImpl;
import com.sep.vox.infrastructure.persistence.entity.RoleJpaEntity;
import com.sep.vox.infrastructure.persistence.entity.UserRoleJpaEntity;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;

@DataJpaTest
@ActiveProfiles("test")
@Import({
    TestContainerConfig.class,
    JpaQuestionPermissionQuery.class,
    JpaUserRoleQueryRepository.class,
    QuestionBankRepositoryImpl.class,
    QuestionTopicRepositoryImpl.class,
    QuestionRepositoryImpl.class,
    UserRepositoryImpl.class,
    SchoolUserRepositoryImpl.class,
    JpaQuestionPermissionQueryTests.TestConfig.class
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class JpaQuestionPermissionQueryTests {

    @Autowired
    private JpaQuestionPermissionQuery query;

    @Autowired
    private UserContextPort userContextPort;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SchoolUserRepository schoolUserRepository;

    @Autowired
    private QuestionBankRepository questionBankRepository;

    @Autowired
    private QuestionTopicRepository questionTopicRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private EntityManager entityManager;

    private UUID schoolId;

    @BeforeEach
    void setUp() {
        schoolId = UUID.randomUUID();
    }

    @Test
    void canEditContent_should_allow_teacher_owner_for_question_bank_draft() {
        var teacher = persistUser("teacher-owner-1@example.com", "0900000001", "TEACHER", schoolId);
        var question = persistQuestion(teacher.getId(), schoolId, QuestionBankOwnerType.SCHOOL,
            QuestionBankStatus.DRAFT, QuestionTopicStatus.DRAFT, QuestionScope.QUESTION_BANK,
            QuestionStatus.DRAFT, QuestionVisibility.BANK_VISIBLE, false);
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(teacher.getId());

        var permitted = query.canEditContent(question.getId());

        assertThat(permitted).isTrue();
    }

    @Test
    void canEditContent_should_reject_teacher_owner_for_classroom_assessment_scope() {
        var teacher = persistUser("teacher-owner-2@example.com", "0900000002", "TEACHER", schoolId);
        var question = persistQuestion(teacher.getId(), schoolId, QuestionBankOwnerType.SCHOOL,
            QuestionBankStatus.DRAFT, QuestionTopicStatus.DRAFT, QuestionScope.CLASSROOM_ASSESSMENT,
            QuestionStatus.DRAFT, QuestionVisibility.BANK_VISIBLE, false);
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(teacher.getId());

        var permitted = query.canEditContent(question.getId());

        assertThat(permitted).isFalse();
    }

    @Test
    void canEditContent_should_reject_teacher_owner_when_question_is_published() {
        var teacher = persistUser("teacher-owner-3@example.com", "0900000003", "TEACHER", schoolId);
        var question = persistQuestion(teacher.getId(), schoolId, QuestionBankOwnerType.SCHOOL,
            QuestionBankStatus.PUBLISHED, QuestionTopicStatus.PUBLISHED, QuestionScope.QUESTION_BANK,
            QuestionStatus.PUBLISHED, QuestionVisibility.BANK_VISIBLE, false);
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(teacher.getId());

        var permitted = query.canEditContent(question.getId());

        assertThat(permitted).isFalse();
    }

    @Test
    void canEditContent_should_reject_teacher_owner_when_question_is_locked() {
        var teacher = persistUser("teacher-owner-4@example.com", "0900000004", "TEACHER", schoolId);
        var question = persistQuestion(teacher.getId(), schoolId, QuestionBankOwnerType.SCHOOL,
            QuestionBankStatus.DRAFT, QuestionTopicStatus.DRAFT, QuestionScope.QUESTION_BANK,
            QuestionStatus.DRAFT, QuestionVisibility.BANK_VISIBLE, true);
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(teacher.getId());

        var permitted = query.canEditContent(question.getId());

        assertThat(permitted).isFalse();
    }

    @Test
    void canEditContent_should_allow_school_admin_for_same_school_question_bank_question() {
        var schoolAdmin = persistUser("school-admin-edit-1@example.com", "0900000101", "SCHOOL_ADMIN", schoolId);
        var question = persistQuestion(UUID.randomUUID(), schoolId, QuestionBankOwnerType.SCHOOL,
            QuestionBankStatus.DRAFT, QuestionTopicStatus.DRAFT, QuestionScope.QUESTION_BANK,
            QuestionStatus.REVISION_REQUESTED, QuestionVisibility.BANK_VISIBLE, false);
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(schoolAdmin.getId());

        var permitted = query.canEditContent(question.getId());

        assertThat(permitted).isTrue();
    }

    @Test
    void canEditContent_should_reject_school_admin_for_other_school_question() {
        var schoolAdmin = persistUser("school-admin-edit-2@example.com", "0900000102", "SCHOOL_ADMIN", schoolId);
        var otherSchoolId = UUID.randomUUID();
        var question = persistQuestion(UUID.randomUUID(), otherSchoolId, QuestionBankOwnerType.SCHOOL,
            QuestionBankStatus.DRAFT, QuestionTopicStatus.DRAFT, QuestionScope.QUESTION_BANK,
            QuestionStatus.DRAFT, QuestionVisibility.BANK_VISIBLE, false);
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(schoolAdmin.getId());

        var permitted = query.canEditContent(question.getId());

        assertThat(permitted).isFalse();
    }

    @Test
    void canEditContent_should_reject_teacher_unrelated_for_other_teachers_question() {
        var unrelatedTeacher = persistUser("teacher-unrelated-edit@example.com", "0900000103", "TEACHER", schoolId);
        var question = persistQuestion(UUID.randomUUID(), schoolId, QuestionBankOwnerType.SCHOOL,
            QuestionBankStatus.DRAFT, QuestionTopicStatus.DRAFT, QuestionScope.QUESTION_BANK,
            QuestionStatus.DRAFT, QuestionVisibility.BANK_VISIBLE, false);
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(unrelatedTeacher.getId());

        var permitted = query.canEditContent(question.getId());

        assertThat(permitted).isFalse();
    }

    @Test
    void canEditContent_should_reject_student() {
        var student = persistUser("student-edit@example.com", "0900000104", "STUDENT", schoolId);
        var question = persistQuestion(UUID.randomUUID(), schoolId, QuestionBankOwnerType.SCHOOL,
            QuestionBankStatus.DRAFT, QuestionTopicStatus.DRAFT, QuestionScope.QUESTION_BANK,
            QuestionStatus.DRAFT, QuestionVisibility.BANK_VISIBLE, false);
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(student.getId());

        var permitted = query.canEditContent(question.getId());

        assertThat(permitted).isFalse();
    }

    @Test
    void canEditContent_should_allow_system_admin_even_when_scope_is_not_question_bank() {
        var systemAdmin = persistUser("system-admin-edit@example.com", "0900000105", "SYSTEM_ADMIN", schoolId);
        var question = persistQuestion(UUID.randomUUID(), schoolId, QuestionBankOwnerType.SCHOOL,
            QuestionBankStatus.PUBLISHED, QuestionTopicStatus.PUBLISHED, QuestionScope.CENTRAL_EXAM_PAPER,
            QuestionStatus.PUBLISHED, QuestionVisibility.EXAM_PAPER_ONLY, true);
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(systemAdmin.getId());

        var permitted = query.canEditContent(question.getId());

        assertThat(permitted).isTrue();
    }

    @Test
    void canReview_should_allow_teacher_owner_to_submit_rejected_question() {
        var teacher = persistUser("teacher-owner-5@example.com", "0900000005", "TEACHER", schoolId);
        var question = persistQuestion(teacher.getId(), schoolId, QuestionBankOwnerType.SCHOOL,
            QuestionBankStatus.DRAFT, QuestionTopicStatus.DRAFT, QuestionScope.QUESTION_BANK,
            QuestionStatus.REJECTED, QuestionVisibility.BANK_VISIBLE, false);
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(teacher.getId());

        var permitted = query.canReview(question.getId(), QuestionStatus.SUBMITTED_FOR_REVIEW);

        assertThat(permitted).isTrue();
    }

    @Test
    void canReview_should_allow_teacher_owner_to_submit_draft_question() {
        var teacher = persistUser("teacher-owner-9@example.com", "0900000013", "TEACHER", schoolId);
        var question = persistQuestion(teacher.getId(), schoolId, QuestionBankOwnerType.SCHOOL,
            QuestionBankStatus.DRAFT, QuestionTopicStatus.DRAFT, QuestionScope.QUESTION_BANK,
            QuestionStatus.DRAFT, QuestionVisibility.BANK_VISIBLE, false);
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(teacher.getId());

        var permitted = query.canReview(question.getId(), QuestionStatus.SUBMITTED_FOR_REVIEW);

        assertThat(permitted).isTrue();
    }

    @Test
    void canReview_should_allow_school_admin_to_approve_same_school_question() {
        var schoolAdmin = persistUser("school-admin-review-1@example.com", "0900000106", "SCHOOL_ADMIN", schoolId);
        var question = persistQuestion(UUID.randomUUID(), schoolId, QuestionBankOwnerType.SCHOOL,
            QuestionBankStatus.PUBLISHED, QuestionTopicStatus.PUBLISHED, QuestionScope.QUESTION_BANK,
            QuestionStatus.SUBMITTED_FOR_REVIEW, QuestionVisibility.REVIEWER_ONLY, false);
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(schoolAdmin.getId());

        var permitted = query.canReview(question.getId(), QuestionStatus.APPROVED);

        assertThat(permitted).isTrue();
    }

    @Test
    void canReview_should_allow_school_admin_to_request_revision_for_same_school_question() {
        var schoolAdmin = persistUser("school-admin-review-3@example.com", "0900000111", "SCHOOL_ADMIN", schoolId);
        var question = persistQuestion(UUID.randomUUID(), schoolId, QuestionBankOwnerType.SCHOOL,
            QuestionBankStatus.PUBLISHED, QuestionTopicStatus.PUBLISHED, QuestionScope.QUESTION_BANK,
            QuestionStatus.SUBMITTED_FOR_REVIEW, QuestionVisibility.REVIEWER_ONLY, false);
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(schoolAdmin.getId());

        var permitted = query.canReview(question.getId(), QuestionStatus.REVISION_REQUESTED);

        assertThat(permitted).isTrue();
    }

    @Test
    void canReview_should_allow_school_admin_to_reject_same_school_question() {
        var schoolAdmin = persistUser("school-admin-review-4@example.com", "0900000112", "SCHOOL_ADMIN", schoolId);
        var question = persistQuestion(UUID.randomUUID(), schoolId, QuestionBankOwnerType.SCHOOL,
            QuestionBankStatus.PUBLISHED, QuestionTopicStatus.PUBLISHED, QuestionScope.QUESTION_BANK,
            QuestionStatus.SUBMITTED_FOR_REVIEW, QuestionVisibility.REVIEWER_ONLY, false);
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(schoolAdmin.getId());

        var permitted = query.canReview(question.getId(), QuestionStatus.REJECTED);

        assertThat(permitted).isTrue();
    }

    @Test
    void canReview_should_allow_teacher_reviewer_to_approve_submitted_reviewer_only_question() {
        var creator = persistUser("teacher-owner-6@example.com", "0900000006", "TEACHER", schoolId);
        var reviewer = persistUser("teacher-reviewer-1@example.com", "0900000011", "TEACHER", schoolId);
        var question = persistQuestion(creator.getId(), schoolId, QuestionBankOwnerType.SCHOOL,
            QuestionBankStatus.PUBLISHED, QuestionTopicStatus.PUBLISHED, QuestionScope.QUESTION_BANK,
            QuestionStatus.SUBMITTED_FOR_REVIEW, QuestionVisibility.REVIEWER_ONLY, false);
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(reviewer.getId());

        var permitted = query.canReview(question.getId(), QuestionStatus.APPROVED);

        assertThat(permitted).isTrue();
    }

    @Test
    void canReview_should_allow_teacher_reviewer_to_request_revision_for_review_queue_question() {
        var creator = persistUser("teacher-owner-10@example.com", "0900000014", "TEACHER", schoolId);
        var reviewer = persistUser("teacher-reviewer-5@example.com", "0900000015", "TEACHER", schoolId);
        var question = persistQuestion(creator.getId(), schoolId, QuestionBankOwnerType.SCHOOL,
            QuestionBankStatus.PUBLISHED, QuestionTopicStatus.PUBLISHED, QuestionScope.QUESTION_BANK,
            QuestionStatus.SUBMITTED_FOR_REVIEW, QuestionVisibility.REVIEWER_ONLY, false);
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(reviewer.getId());

        var permitted = query.canReview(question.getId(), QuestionStatus.REVISION_REQUESTED);

        assertThat(permitted).isTrue();
    }

    @Test
    void canReview_should_allow_teacher_reviewer_to_reject_review_queue_question() {
        var creator = persistUser("teacher-owner-11@example.com", "0900000016", "TEACHER", schoolId);
        var reviewer = persistUser("teacher-reviewer-6@example.com", "0900000017", "TEACHER", schoolId);
        var question = persistQuestion(creator.getId(), schoolId, QuestionBankOwnerType.SCHOOL,
            QuestionBankStatus.PUBLISHED, QuestionTopicStatus.PUBLISHED, QuestionScope.QUESTION_BANK,
            QuestionStatus.SUBMITTED_FOR_REVIEW, QuestionVisibility.REVIEWER_ONLY, false);
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(reviewer.getId());

        var permitted = query.canReview(question.getId(), QuestionStatus.REJECTED);

        assertThat(permitted).isTrue();
    }

    @Test
    void canReview_should_reject_school_admin_for_other_school_question() {
        var schoolAdmin = persistUser("school-admin-review-2@example.com", "0900000107", "SCHOOL_ADMIN", schoolId);
        var otherSchoolId = UUID.randomUUID();
        var question = persistQuestion(UUID.randomUUID(), otherSchoolId, QuestionBankOwnerType.SCHOOL,
            QuestionBankStatus.PUBLISHED, QuestionTopicStatus.PUBLISHED, QuestionScope.QUESTION_BANK,
            QuestionStatus.SUBMITTED_FOR_REVIEW, QuestionVisibility.REVIEWER_ONLY, false);
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(schoolAdmin.getId());

        var permitted = query.canReview(question.getId(), QuestionStatus.APPROVED);

        assertThat(permitted).isFalse();
    }

    @Test
    void canReview_should_reject_teacher_reviewer_when_visibility_is_not_reviewer_only() {
        var creator = persistUser("teacher-owner-7@example.com", "0900000007", "TEACHER", schoolId);
        var reviewer = persistUser("teacher-reviewer-2@example.com", "0900000012", "TEACHER", schoolId);
        var question = persistQuestion(creator.getId(), schoolId, QuestionBankOwnerType.SCHOOL,
            QuestionBankStatus.PUBLISHED, QuestionTopicStatus.PUBLISHED, QuestionScope.QUESTION_BANK,
            QuestionStatus.SUBMITTED_FOR_REVIEW, QuestionVisibility.BANK_VISIBLE, false);
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(reviewer.getId());

        var permitted = query.canReview(question.getId(), QuestionStatus.APPROVED);

        assertThat(permitted).isFalse();
    }

    @Test
    void canReview_should_reject_teacher_unrelated_for_review_action() {
        var unrelatedTeacher = persistUser("teacher-unrelated-review@example.com", "0900000108", "TEACHER", schoolId);
        var question = persistQuestion(UUID.randomUUID(), schoolId, QuestionBankOwnerType.SCHOOL,
            QuestionBankStatus.PUBLISHED, QuestionTopicStatus.PUBLISHED, QuestionScope.QUESTION_BANK,
            QuestionStatus.SUBMITTED_FOR_REVIEW, QuestionVisibility.BANK_VISIBLE, false);
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(unrelatedTeacher.getId());

        var permitted = query.canReview(question.getId(), QuestionStatus.APPROVED);

        assertThat(permitted).isFalse();
    }

    @Test
    void canReview_should_allow_teacher_owner_to_publish_approved_question() {
        var teacher = persistUser("teacher-owner-8@example.com", "0900000008", "TEACHER", schoolId);
        var question = persistQuestion(teacher.getId(), schoolId, QuestionBankOwnerType.SCHOOL,
            QuestionBankStatus.PUBLISHED, QuestionTopicStatus.PUBLISHED, QuestionScope.QUESTION_BANK,
            QuestionStatus.APPROVED, QuestionVisibility.BANK_VISIBLE, false);
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(teacher.getId());

        var permitted = query.canReview(question.getId(), QuestionStatus.PUBLISHED);

        assertThat(permitted).isTrue();
    }

    @Test
    void canReview_should_allow_school_admin_to_restore_archived_same_school_question() {
        var schoolAdmin = persistUser("school-admin-review-5@example.com", "0900000113", "SCHOOL_ADMIN", schoolId);
        var question = persistQuestion(UUID.randomUUID(), schoolId, QuestionBankOwnerType.SCHOOL,
            QuestionBankStatus.PUBLISHED, QuestionTopicStatus.PUBLISHED, QuestionScope.QUESTION_BANK,
            QuestionStatus.ARCHIVED, QuestionVisibility.BANK_VISIBLE, false);
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(schoolAdmin.getId());

        var permitted = query.canReview(question.getId(), QuestionStatus.DRAFT);

        assertThat(permitted).isTrue();
    }

    @Test
    void canReview_should_reject_teacher_reviewer_for_restore_action() {
        var creator = persistUser("teacher-owner-12@example.com", "0900000018", "TEACHER", schoolId);
        var reviewer = persistUser("teacher-reviewer-7@example.com", "0900000019", "TEACHER", schoolId);
        var question = persistQuestion(creator.getId(), schoolId, QuestionBankOwnerType.SCHOOL,
            QuestionBankStatus.PUBLISHED, QuestionTopicStatus.PUBLISHED, QuestionScope.QUESTION_BANK,
            QuestionStatus.ARCHIVED, QuestionVisibility.REVIEWER_ONLY, false);
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(reviewer.getId());

        var permitted = query.canReview(question.getId(), QuestionStatus.DRAFT);

        assertThat(permitted).isFalse();
    }

    @Test
    void canReview_should_reject_student() {
        var student = persistUser("student-review@example.com", "0900000109", "STUDENT", schoolId);
        var question = persistQuestion(UUID.randomUUID(), schoolId, QuestionBankOwnerType.SCHOOL,
            QuestionBankStatus.PUBLISHED, QuestionTopicStatus.PUBLISHED, QuestionScope.QUESTION_BANK,
            QuestionStatus.SUBMITTED_FOR_REVIEW, QuestionVisibility.REVIEWER_ONLY, false);
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(student.getId());

        var permitted = query.canReview(question.getId(), QuestionStatus.APPROVED);

        assertThat(permitted).isFalse();
    }

    @Test
    void canReview_should_allow_system_admin_for_any_review_action() {
        var systemAdmin = persistUser("system-admin-review@example.com", "0900000110", "SYSTEM_ADMIN", schoolId);
        var question = persistQuestion(UUID.randomUUID(), schoolId, QuestionBankOwnerType.SCHOOL,
            QuestionBankStatus.DRAFT, QuestionTopicStatus.DRAFT, QuestionScope.CLASSROOM_ASSESSMENT,
            QuestionStatus.DRAFT, QuestionVisibility.AUTHOR_ONLY, true);
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(systemAdmin.getId());

        var permitted = query.canReview(question.getId(), QuestionStatus.ARCHIVED);

        assertThat(permitted).isTrue();
    }

    private User persistUser(String email, String phone, String roleCode, UUID assignedSchoolId) {
        var user = userRepository.save(new User(
            new Email(email),
            "password-hash",
            new Phone(phone),
            new FullName("Question Tester"),
            null,
            new DateOfBirth(LocalDate.of(2000, 1, 1)),
            "Ho Chi Minh City",
            null,
            UserStatus.ACTIVE,
            OffsetDateTime.now(),
            OffsetDateTime.now(),
            null,
            null
        ));

        var role = findOrCreateRole(roleCode);

        entityManager.persist(new UserRoleJpaEntity(null, user.getId(), role.getId(), OffsetDateTime.now()));
        entityManager.flush();

        schoolUserRepository.save(new SchoolUser(assignedSchoolId, user.getId(), OffsetDateTime.now(), null));
        return user;
    }

    private RoleJpaEntity findOrCreateRole(String roleCode) {
        try {
            return entityManager.createQuery(
                "SELECT r FROM RoleJpaEntity r WHERE r.code = :code",
                RoleJpaEntity.class
            )
                .setParameter("code", roleCode)
                .getSingleResult();
        } catch (NoResultException ex) {
            var role = new RoleJpaEntity(null, roleCode, roleCode, OffsetDateTime.now(), OffsetDateTime.now(), null, null);
            entityManager.persist(role);
            entityManager.flush();
            return role;
        }
    }

    private Question persistQuestion(
            UUID createdBy,
            UUID questionSchoolId,
            QuestionBankOwnerType ownerType,
            QuestionBankStatus bankStatus,
            QuestionTopicStatus topicStatus,
            QuestionScope scope,
            QuestionStatus questionStatus,
            QuestionVisibility visibility,
            boolean locked) {
        var now = OffsetDateTime.now();
        var bank = questionBankRepository.save(new QuestionBank(
            UUID.randomUUID(),
            ownerType == QuestionBankOwnerType.SCHOOL ? questionSchoolId : null,
            "BANK_" + UUID.randomUUID(),
            "Bank",
            null,
            ownerType,
            bankStatus,
            now,
            now,
            createdBy,
            createdBy
        ));
        var topic = questionTopicRepository.save(new QuestionTopic(
            bank.getId(),
            "TOPIC_" + UUID.randomUUID(),
            "Topic",
            null,
            topicStatus,
            now,
            now,
            createdBy,
            createdBy
        ));
        return questionRepository.save(new Question(
            topic.getId(),
            "Q_" + UUID.randomUUID(),
            "Instruction",
            "Question",
            "Prompt",
            "Preparation",
            QuestionType.SHORT_ANSWER,
            10,
            20,
            30,
            scope,
            visibility,
            null,
            locked,
            questionStatus,
            now,
            now,
            createdBy,
            createdBy
        ));
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        UserContextPort userContextPort() {
            return org.mockito.Mockito.mock(UserContextPort.class);
        }
    }
}
