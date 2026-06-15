package com.sep.vox.infrastructure.persistence.query;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.sep.vox.config.TestContainerConfig;
import com.sep.vox.domain.common.PageRequest;
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
import com.sep.vox.domain.dto.QuestionTopicDto;
import com.sep.vox.domain.repository.QuestionBankRepository;
import com.sep.vox.domain.repository.QuestionRepository;
import com.sep.vox.domain.repository.QuestionTopicRepository;
import com.sep.vox.infrastructure.persistence.adapter.QuestionBankRepositoryImpl;
import com.sep.vox.infrastructure.persistence.adapter.QuestionRepositoryImpl;
import com.sep.vox.infrastructure.persistence.adapter.QuestionTopicRepositoryImpl;

@DataJpaTest
@ActiveProfiles("test")
@Import({
    TestContainerConfig.class,
    JpaQuestionReadQueryRepository.class,
    JpaQuestionTopicReadQueryRepository.class,
    QuestionBankRepositoryImpl.class,
    QuestionTopicRepositoryImpl.class,
    QuestionRepositoryImpl.class
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class JpaQuestionReadQueryRepositoryTests {

    @Autowired
    private JpaQuestionReadQueryRepository repository;

    @Autowired
    private JpaQuestionTopicReadQueryRepository topicRepository;

    @Autowired
    private QuestionBankRepository questionBankRepository;

    @Autowired
    private QuestionTopicRepository questionTopicRepository;

    @Autowired
    private QuestionRepository questionRepository;

    private UUID schoolId;
    private UUID otherSchoolId;
    private UUID teacherId;
    private UUID otherTeacherId;

    @BeforeEach
    void setUp() {
        schoolId = UUID.randomUUID();
        otherSchoolId = UUID.randomUUID();
        teacherId = UUID.randomUUID();
        otherTeacherId = UUID.randomUUID();
    }

    @Test
    void findTeacherVisibleQuestions_should_return_only_same_school_published_or_own_questions() {
        persistQuestion(teacherId, schoolId, QuestionBankOwnerType.SCHOOL, QuestionBankStatus.DRAFT, QuestionTopicStatus.DRAFT,
            "OWN_DRAFT", QuestionScope.QUESTION_BANK, QuestionStatus.DRAFT, QuestionVisibility.AUTHOR_ONLY);
        persistQuestion(otherTeacherId, schoolId, QuestionBankOwnerType.SCHOOL, QuestionBankStatus.PUBLISHED, QuestionTopicStatus.PUBLISHED,
            "SAME_SCHOOL_PUBLISHED", QuestionScope.QUESTION_BANK, QuestionStatus.PUBLISHED, QuestionVisibility.BANK_VISIBLE);
        persistQuestion(otherTeacherId, otherSchoolId, QuestionBankOwnerType.SCHOOL, QuestionBankStatus.PUBLISHED, QuestionTopicStatus.PUBLISHED,
            "OTHER_SCHOOL_PUBLISHED", QuestionScope.QUESTION_BANK, QuestionStatus.PUBLISHED, QuestionVisibility.BANK_VISIBLE);

        var result = repository.findTeacherVisibleQuestions(teacherId, schoolId, null, null, null, null, new PageRequest(1, 20));

        assertThat(result.content())
            .extracting(question -> question.code())
            .contains("OWN_DRAFT", "SAME_SCHOOL_PUBLISHED")
            .doesNotContain("OTHER_SCHOOL_PUBLISHED");
    }

    @Test
    void findTeacherVisibleQuestions_should_filter_by_scope_and_status() {
        persistQuestion(teacherId, schoolId, QuestionBankOwnerType.SCHOOL, QuestionBankStatus.PUBLISHED, QuestionTopicStatus.PUBLISHED,
            "BANK_PUBLISHED", QuestionScope.QUESTION_BANK, QuestionStatus.PUBLISHED, QuestionVisibility.BANK_VISIBLE);
        persistQuestion(teacherId, schoolId, QuestionBankOwnerType.SCHOOL, QuestionBankStatus.PUBLISHED, QuestionTopicStatus.PUBLISHED,
            "EXAM_DRAFT", QuestionScope.CENTRAL_EXAM_DRAFT, QuestionStatus.PUBLISHED, QuestionVisibility.BANK_VISIBLE);

        var result = repository.findTeacherVisibleQuestions(
            teacherId, schoolId, "CENTRAL_EXAM_DRAFT", "PUBLISHED", null, null, new PageRequest(1, 20)
        );

        assertThat(result.content()).extracting(question -> question.code()).containsExactly("EXAM_DRAFT");
    }

    @Test
    void findTeacherMyQuestions_should_return_only_current_teachers_questions() {
        persistQuestion(teacherId, schoolId, QuestionBankOwnerType.SCHOOL, QuestionBankStatus.DRAFT, QuestionTopicStatus.DRAFT,
            "MY_Q1", QuestionScope.QUESTION_BANK, QuestionStatus.DRAFT, QuestionVisibility.AUTHOR_ONLY);
        persistQuestion(otherTeacherId, schoolId, QuestionBankOwnerType.SCHOOL, QuestionBankStatus.DRAFT, QuestionTopicStatus.DRAFT,
            "OTHER_Q1", QuestionScope.QUESTION_BANK, QuestionStatus.DRAFT, QuestionVisibility.AUTHOR_ONLY);

        var result = repository.findTeacherMyQuestions(teacherId, new PageRequest(1, 20));

        assertThat(result.content()).extracting(question -> question.code()).containsExactly("MY_Q1");
    }

    @Test
    void findTeacherReviewQueue_should_return_only_same_school_reviewer_only_and_not_own_questions() {
        persistQuestion(otherTeacherId, schoolId, QuestionBankOwnerType.SCHOOL, QuestionBankStatus.PUBLISHED, QuestionTopicStatus.PUBLISHED,
            "REVIEWABLE", QuestionScope.QUESTION_BANK, QuestionStatus.SUBMITTED_FOR_REVIEW, QuestionVisibility.REVIEWER_ONLY);
        persistQuestion(teacherId, schoolId, QuestionBankOwnerType.SCHOOL, QuestionBankStatus.PUBLISHED, QuestionTopicStatus.PUBLISHED,
            "OWN_REVIEWABLE", QuestionScope.QUESTION_BANK, QuestionStatus.SUBMITTED_FOR_REVIEW, QuestionVisibility.REVIEWER_ONLY);
        persistQuestion(otherTeacherId, otherSchoolId, QuestionBankOwnerType.SCHOOL, QuestionBankStatus.PUBLISHED, QuestionTopicStatus.PUBLISHED,
            "OTHER_SCHOOL_REVIEWABLE", QuestionScope.QUESTION_BANK, QuestionStatus.SUBMITTED_FOR_REVIEW, QuestionVisibility.REVIEWER_ONLY);

        var result = repository.findTeacherReviewQueue(teacherId, schoolId, new PageRequest(1, 20));

        assertThat(result.content())
            .extracting(question -> question.code())
            .containsExactly("REVIEWABLE");
    }

    @Test
    void findSchoolVisibleQuestions_should_exclude_author_only_and_return_system_published_only() {
        persistQuestion(otherTeacherId, schoolId, QuestionBankOwnerType.SCHOOL, QuestionBankStatus.DRAFT, QuestionTopicStatus.DRAFT,
            "SCHOOL_DRAFT", QuestionScope.QUESTION_BANK, QuestionStatus.DRAFT, QuestionVisibility.AUTHOR_ONLY);
        persistQuestion(otherTeacherId, schoolId, QuestionBankOwnerType.SCHOOL, QuestionBankStatus.DRAFT, QuestionTopicStatus.DRAFT,
            "SCHOOL_REVIEWER_ONLY", QuestionScope.QUESTION_BANK, QuestionStatus.SUBMITTED_FOR_REVIEW, QuestionVisibility.REVIEWER_ONLY);
        persistQuestion(UUID.randomUUID(), null, QuestionBankOwnerType.SYSTEM, QuestionBankStatus.PUBLISHED, QuestionTopicStatus.PUBLISHED,
            "SYSTEM_PUBLISHED", QuestionScope.QUESTION_BANK, QuestionStatus.PUBLISHED, QuestionVisibility.BANK_VISIBLE);
        persistQuestion(otherTeacherId, otherSchoolId, QuestionBankOwnerType.SCHOOL, QuestionBankStatus.PUBLISHED, QuestionTopicStatus.PUBLISHED,
            "OTHER_SCHOOL", QuestionScope.QUESTION_BANK, QuestionStatus.PUBLISHED, QuestionVisibility.BANK_VISIBLE);

        var result = repository.findSchoolVisibleQuestions(schoolId, null, null, null, null, new PageRequest(1, 20));

        assertThat(result.content())
            .extracting(question -> question.code())
            .contains("SCHOOL_REVIEWER_ONLY", "SYSTEM_PUBLISHED")
            .doesNotContain("SCHOOL_DRAFT")
            .doesNotContain("OTHER_SCHOOL");
    }

    @Test
    void findSchoolReviewQueue_should_return_only_same_school_submitted_reviewer_only_questions() {
        persistQuestion(otherTeacherId, schoolId, QuestionBankOwnerType.SCHOOL, QuestionBankStatus.PUBLISHED, QuestionTopicStatus.PUBLISHED,
            "SCHOOL_QUEUE", QuestionScope.QUESTION_BANK, QuestionStatus.SUBMITTED_FOR_REVIEW, QuestionVisibility.REVIEWER_ONLY);
        persistQuestion(otherTeacherId, otherSchoolId, QuestionBankOwnerType.SCHOOL, QuestionBankStatus.PUBLISHED, QuestionTopicStatus.PUBLISHED,
            "OTHER_SCHOOL_QUEUE", QuestionScope.QUESTION_BANK, QuestionStatus.SUBMITTED_FOR_REVIEW, QuestionVisibility.REVIEWER_ONLY);
        persistQuestion(otherTeacherId, schoolId, QuestionBankOwnerType.SCHOOL, QuestionBankStatus.PUBLISHED, QuestionTopicStatus.PUBLISHED,
            "SCHOOL_NOT_REVIEWER_ONLY", QuestionScope.QUESTION_BANK, QuestionStatus.SUBMITTED_FOR_REVIEW, QuestionVisibility.BANK_VISIBLE);

        var result = repository.findSchoolReviewQueue(schoolId, new PageRequest(1, 20));

        assertThat(result.content())
            .extracting(question -> question.code())
            .containsExactly("SCHOOL_QUEUE");
    }

    @Test
    void findSchoolVisibleQuestions_should_include_central_exam_draft_in_same_school_when_filtered() {
        persistQuestion(otherTeacherId, schoolId, QuestionBankOwnerType.SCHOOL, QuestionBankStatus.PUBLISHED, QuestionTopicStatus.PUBLISHED,
            "SCHOOL_EXAM_DRAFT", QuestionScope.CENTRAL_EXAM_DRAFT, QuestionStatus.PUBLISHED, QuestionVisibility.BANK_VISIBLE);

        var result = repository.findSchoolVisibleQuestions(schoolId, "CENTRAL_EXAM_DRAFT", "PUBLISHED", null, null, new PageRequest(1, 20));

        assertThat(result.content()).extracting(question -> question.code()).containsExactly("SCHOOL_EXAM_DRAFT");
    }

    @Test
    void findAdminQuestions_should_exclude_archived_when_flag_is_false() {
        persistQuestion(teacherId, schoolId, QuestionBankOwnerType.SCHOOL, QuestionBankStatus.PUBLISHED, QuestionTopicStatus.PUBLISHED,
            "ADMIN_VISIBLE", QuestionScope.QUESTION_BANK, QuestionStatus.PUBLISHED, QuestionVisibility.BANK_VISIBLE);
        persistQuestion(teacherId, schoolId, QuestionBankOwnerType.SCHOOL, QuestionBankStatus.ARCHIVED, QuestionTopicStatus.PUBLISHED,
            "ADMIN_ARCHIVED_BANK", QuestionScope.QUESTION_BANK, QuestionStatus.PUBLISHED, QuestionVisibility.BANK_VISIBLE);

        var result = repository.findAdminQuestions(false, null, null, new PageRequest(1, 20));

        assertThat(result.content()).extracting(question -> question.code()).contains("ADMIN_VISIBLE").doesNotContain("ADMIN_ARCHIVED_BANK");
    }

    @Test
    void findAdminQuestions_should_include_archived_when_flag_is_true_and_filter_by_keyword() {
        persistQuestion(teacherId, schoolId, QuestionBankOwnerType.SCHOOL, QuestionBankStatus.ARCHIVED, QuestionTopicStatus.PUBLISHED,
            "ADMIN_KEYWORD_MATCH", QuestionScope.QUESTION_BANK, QuestionStatus.PUBLISHED, QuestionVisibility.BANK_VISIBLE);

        var result = repository.findAdminQuestions(true, null, "admin_keyword_match", new PageRequest(1, 20));

        assertThat(result.content()).extracting(question -> question.code()).containsExactly("ADMIN_KEYWORD_MATCH");
    }

    @Test
    void findAdminReviewQueue_should_return_all_submitted_for_review_questions() {
        persistQuestion(teacherId, schoolId, QuestionBankOwnerType.SCHOOL, QuestionBankStatus.PUBLISHED, QuestionTopicStatus.PUBLISHED,
            "ADMIN_QUEUE_1", QuestionScope.QUESTION_BANK, QuestionStatus.SUBMITTED_FOR_REVIEW, QuestionVisibility.REVIEWER_ONLY);
        persistQuestion(otherTeacherId, otherSchoolId, QuestionBankOwnerType.SCHOOL, QuestionBankStatus.PUBLISHED, QuestionTopicStatus.PUBLISHED,
            "ADMIN_QUEUE_2", QuestionScope.CENTRAL_EXAM_DRAFT, QuestionStatus.SUBMITTED_FOR_REVIEW, QuestionVisibility.BANK_VISIBLE);

        var result = repository.findAdminReviewQueue(new PageRequest(1, 20));

        assertThat(result.content()).extracting(question -> question.code()).contains("ADMIN_QUEUE_1", "ADMIN_QUEUE_2");
    }

    @Test
    void findTeacherBankTopics_should_return_only_published_topics_in_same_school_bank() {
        var sameSchoolBank = persistBank(UUID.randomUUID(), schoolId, QuestionBankOwnerType.SCHOOL, QuestionBankStatus.PUBLISHED, "TBANK");
        persistTopic(sameSchoolBank.getId(), teacherId, "TOPIC_VISIBLE", QuestionTopicStatus.PUBLISHED);

        var result = topicRepository.findTeacherBankTopics(sameSchoolBank.getId(), teacherId, schoolId, new PageRequest(1, 20));

        assertThat(result.content()).extracting(QuestionTopicDto::code).containsExactly("TOPIC_VISIBLE");
    }

    @Test
    void findTeacherTopicQuestions_should_return_published_same_school_questions_only() {
        var bank = persistBank(UUID.randomUUID(), schoolId, QuestionBankOwnerType.SCHOOL, QuestionBankStatus.PUBLISHED, "TBANK2");
        var topic = persistTopic(bank.getId(), teacherId, "TOPIC_Q", QuestionTopicStatus.PUBLISHED);
        persistQuestion(topic.getId(), teacherId, "VISIBLE_Q", QuestionScope.QUESTION_BANK, QuestionStatus.PUBLISHED, QuestionVisibility.BANK_VISIBLE);
        persistQuestion(topic.getId(), otherTeacherId, "HIDDEN_REVIEW_Q", QuestionScope.QUESTION_BANK, QuestionStatus.SUBMITTED_FOR_REVIEW, QuestionVisibility.REVIEWER_ONLY);

        var result = topicRepository.findTeacherTopicQuestions(bank.getId(), topic.getId(), teacherId, schoolId, null, "PUBLISHED", null, null, new PageRequest(1, 20));

        assertThat(result.content()).extracting(question -> question.code()).containsExactly("VISIBLE_Q");
    }

    @Test
    void findSchoolBankTopics_should_return_school_topics_and_system_published_topics() {
        var schoolBank = persistBank(UUID.randomUUID(), schoolId, QuestionBankOwnerType.SCHOOL, QuestionBankStatus.DRAFT, "SBANK");
        persistTopic(schoolBank.getId(), teacherId, "SCHOOL_DRAFT_TOPIC", QuestionTopicStatus.DRAFT);

        var result = topicRepository.findSchoolBankTopics(schoolBank.getId(), schoolId, new PageRequest(1, 20));

        assertThat(result.content()).extracting(QuestionTopicDto::code).containsExactly("SCHOOL_DRAFT_TOPIC");
    }

    @Test
    void findSchoolTopicQuestions_should_exclude_author_only_and_include_other_visible_same_school_questions() {
        var bank = persistBank(UUID.randomUUID(), schoolId, QuestionBankOwnerType.SCHOOL, QuestionBankStatus.DRAFT, "SBANK2");
        var topic = persistTopic(bank.getId(), teacherId, "SCHOOL_TOPIC", QuestionTopicStatus.DRAFT);
        persistQuestion(topic.getId(), teacherId, "SCHOOL_Q", QuestionScope.QUESTION_BANK, QuestionStatus.DRAFT, QuestionVisibility.AUTHOR_ONLY);
        persistQuestion(topic.getId(), otherTeacherId, "SCHOOL_REVIEW_Q", QuestionScope.QUESTION_BANK, QuestionStatus.SUBMITTED_FOR_REVIEW, QuestionVisibility.REVIEWER_ONLY);

        var result = topicRepository.findSchoolTopicQuestions(bank.getId(), topic.getId(), schoolId, null, null, null, null, new PageRequest(1, 20));

        assertThat(result.content()).extracting(question -> question.code())
            .containsExactly("SCHOOL_REVIEW_Q");
    }

    @Test
    void findAdminBankTopics_should_honor_include_archived_flag() {
        var bank = persistBank(UUID.randomUUID(), schoolId, QuestionBankOwnerType.SCHOOL, QuestionBankStatus.PUBLISHED, "ABANK");
        persistTopic(bank.getId(), teacherId, "ADMIN_PUBLISHED_TOPIC", QuestionTopicStatus.PUBLISHED);
        persistTopic(bank.getId(), teacherId, "ADMIN_ARCHIVED_TOPIC", QuestionTopicStatus.ARCHIVED);

        var resultWithoutArchived = topicRepository.findAdminBankTopics(bank.getId(), false, new PageRequest(1, 20));
        var resultWithArchived = topicRepository.findAdminBankTopics(bank.getId(), true, new PageRequest(1, 20));

        assertThat(resultWithoutArchived.content()).extracting(QuestionTopicDto::code).containsExactly("ADMIN_PUBLISHED_TOPIC");
        assertThat(resultWithArchived.content()).extracting(QuestionTopicDto::code)
            .contains("ADMIN_PUBLISHED_TOPIC", "ADMIN_ARCHIVED_TOPIC");
    }

    @Test
    void findAdminTopicQuestions_should_honor_scope_and_include_archived_flag() {
        var bank = persistBank(UUID.randomUUID(), schoolId, QuestionBankOwnerType.SCHOOL, QuestionBankStatus.PUBLISHED, "ABANK2");
        var topic = persistTopic(bank.getId(), teacherId, "ADMIN_TOPIC", QuestionTopicStatus.PUBLISHED);
        persistQuestion(topic.getId(), teacherId, "ADMIN_Q1", QuestionScope.QUESTION_BANK, QuestionStatus.PUBLISHED, QuestionVisibility.BANK_VISIBLE);
        persistQuestion(topic.getId(), teacherId, "ADMIN_Q2", QuestionScope.CENTRAL_EXAM_DRAFT, QuestionStatus.PUBLISHED, QuestionVisibility.BANK_VISIBLE);

        var result = topicRepository.findAdminTopicQuestions(bank.getId(), topic.getId(), false, "CENTRAL_EXAM_DRAFT", "PUBLISHED", null, null, new PageRequest(1, 20));

        assertThat(result.content()).extracting(question -> question.code()).containsExactly("ADMIN_Q2");
    }

    @Test
    void findVisibleQuestion_should_return_empty_for_teacher_viewing_other_school_published_question() {
        var bank = persistBank(UUID.randomUUID(), otherSchoolId, QuestionBankOwnerType.SCHOOL, QuestionBankStatus.PUBLISHED, "VBANK");
        var topic = persistTopic(bank.getId(), otherTeacherId, "VTOPIC", QuestionTopicStatus.PUBLISHED);
        var question = persistQuestion(topic.getId(), otherTeacherId, "VQ", QuestionScope.QUESTION_BANK, QuestionStatus.PUBLISHED, QuestionVisibility.BANK_VISIBLE);

        var result = repository.findVisibleQuestion(question.getId(), teacherId, "TEACHER", schoolId);

        assertThat(result).isEmpty();
    }

    @Test
    void findVisibleQuestion_should_return_system_published_question_for_teacher() {
        var bank = persistBank(UUID.randomUUID(), null, QuestionBankOwnerType.SYSTEM, QuestionBankStatus.PUBLISHED, "VBANK_SYS");
        var topic = persistTopic(bank.getId(), otherTeacherId, "VTOPIC_SYS", QuestionTopicStatus.PUBLISHED);
        var question = persistQuestion(topic.getId(), otherTeacherId, "VQ_SYS", QuestionScope.QUESTION_BANK, QuestionStatus.PUBLISHED, QuestionVisibility.BANK_VISIBLE);

        var result = repository.findVisibleQuestion(question.getId(), teacherId, "TEACHER", schoolId);

        assertThat(result).isPresent();
        assertThat(result.get().code()).isEqualTo("VQ_SYS");
    }

    @Test
    void findVisibleQuestion_should_reject_author_only_question_for_school_admin_in_same_school() {
        var bank = persistBank(UUID.randomUUID(), schoolId, QuestionBankOwnerType.SCHOOL, QuestionBankStatus.DRAFT, "VBANK2");
        var topic = persistTopic(bank.getId(), otherTeacherId, "VTOPIC2", QuestionTopicStatus.DRAFT);
        var question = persistQuestion(topic.getId(), otherTeacherId, "VQ2", QuestionScope.QUESTION_BANK, QuestionStatus.DRAFT, QuestionVisibility.AUTHOR_ONLY);

        var result = repository.findVisibleQuestion(question.getId(), UUID.randomUUID(), "SCHOOL_ADMIN", schoolId);

        assertThat(result).isEmpty();
    }

    private Question persistQuestion(
            UUID createdBy,
            UUID questionSchoolId,
            QuestionBankOwnerType ownerType,
            QuestionBankStatus bankStatus,
            QuestionTopicStatus topicStatus,
            String code,
            QuestionScope scope,
            QuestionStatus questionStatus,
            QuestionVisibility visibility) {
        var now = OffsetDateTime.now();
        var bank = questionBankRepository.save(new QuestionBank(
            UUID.randomUUID(),
            questionSchoolId,
            "BANK_" + code,
            "Bank " + code,
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
            "TOPIC_" + code,
            "Topic " + code,
            null,
            topicStatus,
            now,
            now,
            createdBy,
            createdBy
        ));
        return questionRepository.save(new Question(
            topic.getId(),
            code,
            "Instruction",
            "Question " + code,
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

    private QuestionBank persistBank(UUID createdBy, UUID schoolIdValue, QuestionBankOwnerType ownerType, QuestionBankStatus status, String code) {
        var now = OffsetDateTime.now();
        return questionBankRepository.save(new QuestionBank(
            UUID.randomUUID(),
            schoolIdValue,
            code,
            "Bank " + code,
            null,
            ownerType,
            status,
            now,
            now,
            createdBy,
            createdBy
        ));
    }

    private QuestionTopic persistTopic(UUID bankId, UUID createdBy, String code, QuestionTopicStatus status) {
        var now = OffsetDateTime.now();
        return questionTopicRepository.save(new QuestionTopic(
            bankId,
            code,
            "Topic " + code,
            null,
            status,
            now,
            now,
            createdBy,
            createdBy
        ));
    }

    private Question persistQuestion(
            UUID topicId,
            UUID createdBy,
            String code,
            QuestionScope scope,
            QuestionStatus status,
            QuestionVisibility visibility) {
        var now = OffsetDateTime.now();
        return questionRepository.save(new Question(
            topicId,
            code,
            "Instruction",
            "Question " + code,
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
            status,
            now,
            now,
            createdBy,
            createdBy
        ));
    }
}
