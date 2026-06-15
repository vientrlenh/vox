package com.sep.vox.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.sep.vox.config.ContainerTestConfig;
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
import com.sep.vox.domain.repository.QuestionBankRepository;
import com.sep.vox.domain.repository.QuestionRepository;
import com.sep.vox.domain.repository.QuestionTopicRepository;
import com.sep.vox.infrastructure.persistence.adapter.QuestionBankRepositoryImpl;
import com.sep.vox.infrastructure.persistence.adapter.QuestionRepositoryImpl;
import com.sep.vox.infrastructure.persistence.adapter.QuestionTopicRepositoryImpl;

@DataJpaTest
@ActiveProfiles("test")
@Import({ QuestionBankRepositoryImpl.class, QuestionTopicRepositoryImpl.class, QuestionRepositoryImpl.class })
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class QuestionRepositoryTests extends ContainerTestConfig {

    @Autowired
    private QuestionBankRepository questionBankRepository;

    @Autowired
    private QuestionTopicRepository questionTopicRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Test
    void when_save_and_findByTopicId_then_returns_questions() {
        var topic = persistTopic("BANK_Q", "TOPIC_Q");
        questionRepository.save(question(topic.getId(), "Q_01", null));

        var found = questionRepository.findByTopicId(topic.getId());

        assertThat(found).hasSize(1);
        assertThat(found.get(0).getCode()).isEqualTo("Q_01");
    }

    @Test
    void when_existsBySourceQuestionId_then_returns_true_for_copied_question() {
        var topic = persistTopic("BANK_COPY", "TOPIC_COPY");
        var sourceId = UUID.randomUUID();
        questionRepository.save(question(topic.getId(), "Q_02", sourceId));

        assertThat(questionRepository.existsBySourceQuestionId(sourceId)).isTrue();
    }

    @Test
    void when_findAll_then_returns_page() {
        var topic = persistTopic("BANK_PAGE", "TOPIC_PAGE");
        questionRepository.save(question(topic.getId(), "Q_03", null));

        var page = questionRepository.findAll(new PageRequest(1, 10));

        assertThat(page.content()).isNotEmpty();
    }

    private QuestionTopic persistTopic(String bankCode, String topicCode) {
        var now = OffsetDateTime.now();
        var bank = questionBankRepository.save(new QuestionBank(
            UUID.randomUUID(), null, bankCode, bankCode, null, QuestionBankOwnerType.SYSTEM,
            QuestionBankStatus.DRAFT, now, now, UUID.randomUUID(), UUID.randomUUID()
        ));
        return questionTopicRepository.save(new QuestionTopic(
            bank.getId(), topicCode, topicCode, null, QuestionTopicStatus.DRAFT,
            now, now, UUID.randomUUID(), UUID.randomUUID()
        ));
    }

    private Question question(UUID topicId, String code, UUID sourceQuestionId) {
        var now = OffsetDateTime.now();
        return new Question(topicId, code, "Instruction", "Question", "Prompt", "Preparation",
            QuestionType.SHORT_ANSWER, 10, 20, 30, QuestionScope.QUESTION_BANK, QuestionVisibility.BANK_VISIBLE,
            sourceQuestionId, false, QuestionStatus.DRAFT, now, now, UUID.randomUUID(), UUID.randomUUID());
    }
}
