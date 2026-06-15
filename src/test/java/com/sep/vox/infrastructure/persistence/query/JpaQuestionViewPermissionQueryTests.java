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
    JpaQuestionViewPermissionQuery.class,
    JpaUserRoleQueryRepository.class,
    QuestionBankRepositoryImpl.class,
    QuestionTopicRepositoryImpl.class,
    QuestionRepositoryImpl.class,
    UserRepositoryImpl.class,
    SchoolUserRepositoryImpl.class,
    JpaQuestionViewPermissionQueryTests.TestConfig.class
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class JpaQuestionViewPermissionQueryTests {

    @Autowired
    private JpaQuestionViewPermissionQuery query;

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
    void canViewQuestionDetail_should_allow_school_admin_to_view_system_published_bank_visible_question() {
        var schoolAdmin = persistUser("school-admin-1@example.com", "0910000001", "SCHOOL_ADMIN", schoolId);
        var question = persistQuestion(UUID.randomUUID(), null, QuestionBankOwnerType.SYSTEM,
            QuestionBankStatus.PUBLISHED, QuestionTopicStatus.PUBLISHED, QuestionScope.QUESTION_BANK,
            QuestionStatus.PUBLISHED, QuestionVisibility.BANK_VISIBLE);
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(schoolAdmin.getId());

        var permitted = query.canViewQuestionDetail(question.getId());

        assertThat(permitted).isTrue();
    }

    @Test
    void canViewQuestionDetail_should_reject_school_admin_for_system_unpublished_question() {
        var schoolAdmin = persistUser("school-admin-2@example.com", "0910000002", "SCHOOL_ADMIN", schoolId);
        var question = persistQuestion(UUID.randomUUID(), null, QuestionBankOwnerType.SYSTEM,
            QuestionBankStatus.DRAFT, QuestionTopicStatus.DRAFT, QuestionScope.QUESTION_BANK,
            QuestionStatus.DRAFT, QuestionVisibility.BANK_VISIBLE);
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(schoolAdmin.getId());

        var permitted = query.canViewQuestionDetail(question.getId());

        assertThat(permitted).isFalse();
    }

    @Test
    void canViewQuestionDetail_should_reject_school_admin_for_other_school_question_bank_question() {
        var schoolAdmin = persistUser("school-admin-3@example.com", "0910000014", "SCHOOL_ADMIN", schoolId);
        var otherSchoolId = UUID.randomUUID();
        var question = persistQuestion(UUID.randomUUID(), otherSchoolId, QuestionBankOwnerType.SCHOOL,
            QuestionBankStatus.PUBLISHED, QuestionTopicStatus.PUBLISHED, QuestionScope.QUESTION_BANK,
            QuestionStatus.PUBLISHED, QuestionVisibility.BANK_VISIBLE);
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(schoolAdmin.getId());

        var permitted = query.canViewQuestionDetail(question.getId());

        assertThat(permitted).isFalse();
    }

    @Test
    void canViewQuestionDetail_should_reject_school_admin_for_same_school_author_only_question() {
        var schoolAdmin = persistUser("school-admin-4@example.com", "0910000022", "SCHOOL_ADMIN", schoolId);
        var creator = persistUser("teacher-owner-17@example.com", "0910000023", "TEACHER", schoolId);
        var question = persistQuestion(creator.getId(), schoolId, QuestionBankOwnerType.SCHOOL,
            QuestionBankStatus.DRAFT, QuestionTopicStatus.DRAFT, QuestionScope.QUESTION_BANK,
            QuestionStatus.DRAFT, QuestionVisibility.AUTHOR_ONLY);
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(schoolAdmin.getId());

        var permitted = query.canViewQuestionDetail(question.getId());

        assertThat(permitted).isFalse();
    }

    @Test
    void canViewQuestionDetail_should_allow_teacher_reviewer_for_submitted_reviewer_only_question() {
        var creator = persistUser("teacher-owner-9@example.com", "0910000003", "TEACHER", schoolId);
        var reviewer = persistUser("teacher-reviewer-3@example.com", "0910000004", "TEACHER", schoolId);
        var question = persistQuestion(creator.getId(), schoolId, QuestionBankOwnerType.SCHOOL,
            QuestionBankStatus.PUBLISHED, QuestionTopicStatus.PUBLISHED, QuestionScope.QUESTION_BANK,
            QuestionStatus.SUBMITTED_FOR_REVIEW, QuestionVisibility.REVIEWER_ONLY);
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(reviewer.getId());

        var permitted = query.canViewQuestionDetail(question.getId());

        assertThat(permitted).isTrue();
    }

    @Test
    void canViewQuestionDetail_should_reject_teacher_reviewer_for_other_school_review_queue_question() {
        var reviewer = persistUser("teacher-reviewer-4@example.com", "0910000015", "TEACHER", schoolId);
        var otherSchoolId = UUID.randomUUID();
        var creator = persistUser("teacher-owner-14@example.com", "0910000016", "TEACHER", otherSchoolId);
        var question = persistQuestion(creator.getId(), otherSchoolId, QuestionBankOwnerType.SCHOOL,
            QuestionBankStatus.PUBLISHED, QuestionTopicStatus.PUBLISHED, QuestionScope.QUESTION_BANK,
            QuestionStatus.SUBMITTED_FOR_REVIEW, QuestionVisibility.REVIEWER_ONLY);
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(reviewer.getId());

        var permitted = query.canViewQuestionDetail(question.getId());

        assertThat(permitted).isFalse();
    }

    @Test
    void canViewQuestionDetail_should_allow_teacher_owner_for_classroom_assessment() {
        var teacher = persistUser("teacher-owner-10@example.com", "0910000005", "TEACHER", schoolId);
        var question = persistQuestion(teacher.getId(), schoolId, QuestionBankOwnerType.SCHOOL,
            QuestionBankStatus.DRAFT, QuestionTopicStatus.DRAFT, QuestionScope.CLASSROOM_ASSESSMENT,
            QuestionStatus.DRAFT, QuestionVisibility.AUTHOR_ONLY);
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(teacher.getId());

        var permitted = query.canViewQuestionDetail(question.getId());

        assertThat(permitted).isTrue();
    }

    @Test
    void canViewQuestionDetail_should_allow_teacher_owner_for_archived_own_question() {
        var teacher = persistUser("teacher-owner-18@example.com", "0910000024", "TEACHER", schoolId);
        var question = persistQuestion(teacher.getId(), schoolId, QuestionBankOwnerType.SCHOOL,
            QuestionBankStatus.PUBLISHED, QuestionTopicStatus.PUBLISHED, QuestionScope.QUESTION_BANK,
            QuestionStatus.ARCHIVED, QuestionVisibility.AUTHOR_ONLY);
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(teacher.getId());

        var permitted = query.canViewQuestionDetail(question.getId());

        assertThat(permitted).isTrue();
    }

    @Test
    void canViewQuestionDetail_should_allow_school_admin_for_archived_same_school_question() {
        var schoolAdmin = persistUser("school-admin-5@example.com", "0910000025", "SCHOOL_ADMIN", schoolId);
        var question = persistQuestion(UUID.randomUUID(), schoolId, QuestionBankOwnerType.SCHOOL,
            QuestionBankStatus.PUBLISHED, QuestionTopicStatus.PUBLISHED, QuestionScope.QUESTION_BANK,
            QuestionStatus.ARCHIVED, QuestionVisibility.BANK_VISIBLE);
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(schoolAdmin.getId());

        var permitted = query.canViewQuestionDetail(question.getId());

        assertThat(permitted).isTrue();
    }

    @Test
    void canViewQuestionDetail_should_allow_system_admin_for_archived_question() {
        var systemAdmin = persistUser("system-admin-1@example.com", "0910000026", "SYSTEM_ADMIN", schoolId);
        var question = persistQuestion(UUID.randomUUID(), schoolId, QuestionBankOwnerType.SCHOOL,
            QuestionBankStatus.PUBLISHED, QuestionTopicStatus.PUBLISHED, QuestionScope.QUESTION_BANK,
            QuestionStatus.ARCHIVED, QuestionVisibility.AUTHOR_ONLY);
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(systemAdmin.getId());

        var permitted = query.canViewQuestionDetail(question.getId());

        assertThat(permitted).isTrue();
    }

    @Test
    void canViewQuestionDetail_should_reject_unrelated_teacher_for_classroom_assessment() {
        var creator = persistUser("teacher-owner-11@example.com", "0910000006", "TEACHER", schoolId);
        var unrelated = persistUser("teacher-other-1@example.com", "0910000007", "TEACHER", schoolId);
        var question = persistQuestion(creator.getId(), schoolId, QuestionBankOwnerType.SCHOOL,
            QuestionBankStatus.DRAFT, QuestionTopicStatus.DRAFT, QuestionScope.CLASSROOM_ASSESSMENT,
            QuestionStatus.DRAFT, QuestionVisibility.AUTHOR_ONLY);
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(unrelated.getId());

        var permitted = query.canViewQuestionDetail(question.getId());

        assertThat(permitted).isFalse();
    }

    @Test
    void canViewQuestionDetail_should_allow_unrelated_teacher_for_same_school_published_bank_visible_question_bank_question() {
        var creator = persistUser("teacher-owner-15@example.com", "0910000017", "TEACHER", schoolId);
        var unrelated = persistUser("teacher-other-4@example.com", "0910000018", "TEACHER", schoolId);
        var question = persistQuestion(creator.getId(), schoolId, QuestionBankOwnerType.SCHOOL,
            QuestionBankStatus.PUBLISHED, QuestionTopicStatus.PUBLISHED, QuestionScope.QUESTION_BANK,
            QuestionStatus.PUBLISHED, QuestionVisibility.BANK_VISIBLE);
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(unrelated.getId());

        var permitted = query.canViewQuestionDetail(question.getId());

        assertThat(permitted).isTrue();
    }

    @Test
    void canViewQuestionDetail_should_reject_unrelated_teacher_for_other_school_published_question_bank_question() {
        var unrelated = persistUser("teacher-other-5@example.com", "0910000019", "TEACHER", schoolId);
        var otherSchoolId = UUID.randomUUID();
        var creator = persistUser("teacher-owner-16@example.com", "0910000020", "TEACHER", otherSchoolId);
        var question = persistQuestion(creator.getId(), otherSchoolId, QuestionBankOwnerType.SCHOOL,
            QuestionBankStatus.PUBLISHED, QuestionTopicStatus.PUBLISHED, QuestionScope.QUESTION_BANK,
            QuestionStatus.PUBLISHED, QuestionVisibility.BANK_VISIBLE);
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(unrelated.getId());

        var permitted = query.canViewQuestionDetail(question.getId());

        assertThat(permitted).isFalse();
    }

    @Test
    void canViewQuestionDetail_should_reject_student_for_question() {
        var student = persistUser("student-1@example.com", "0910000021", "STUDENT", schoolId);
        var question = persistQuestion(UUID.randomUUID(), null, QuestionBankOwnerType.SYSTEM,
            QuestionBankStatus.PUBLISHED, QuestionTopicStatus.PUBLISHED, QuestionScope.QUESTION_BANK,
            QuestionStatus.PUBLISHED, QuestionVisibility.BANK_VISIBLE);
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(student.getId());

        var permitted = query.canViewQuestionDetail(question.getId());

        assertThat(permitted).isFalse();
    }

    @Test
    void canViewQuestionDetail_should_allow_teacher_unrelated_for_published_central_exam_draft_in_same_school() {
        var creator = persistUser("teacher-owner-12@example.com", "0910000008", "TEACHER", schoolId);
        var unrelated = persistUser("teacher-other-2@example.com", "0910000009", "TEACHER", schoolId);
        var question = persistQuestion(creator.getId(), schoolId, QuestionBankOwnerType.SCHOOL,
            QuestionBankStatus.PUBLISHED, QuestionTopicStatus.PUBLISHED, QuestionScope.CENTRAL_EXAM_DRAFT,
            QuestionStatus.PUBLISHED, QuestionVisibility.BANK_VISIBLE);
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(unrelated.getId());

        var permitted = query.canViewQuestionDetail(question.getId());

        assertThat(permitted).isTrue();
    }

    @Test
    void canViewQuestionDetail_should_allow_only_owner_teacher_for_central_exam_paper() {
        var creator = persistUser("teacher-owner-13@example.com", "0910000010", "TEACHER", schoolId);
        var unrelated = persistUser("teacher-other-3@example.com", "0910000013", "TEACHER", schoolId);
        var question = persistQuestion(creator.getId(), schoolId, QuestionBankOwnerType.SCHOOL,
            QuestionBankStatus.PUBLISHED, QuestionTopicStatus.PUBLISHED, QuestionScope.CENTRAL_EXAM_PAPER,
            QuestionStatus.PUBLISHED, QuestionVisibility.EXAM_PAPER_ONLY);

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(creator.getId());
        assertThat(query.canViewQuestionDetail(question.getId())).isTrue();

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(unrelated.getId());
        assertThat(query.canViewQuestionDetail(question.getId())).isFalse();
    }

    private User persistUser(String email, String phone, String roleCode, UUID assignedSchoolId) {
        var user = userRepository.save(new User(
            new Email(email),
            "password-hash",
            new Phone(phone),
            new FullName("Question Viewer"),
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
            QuestionVisibility visibility) {
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
            false,
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
