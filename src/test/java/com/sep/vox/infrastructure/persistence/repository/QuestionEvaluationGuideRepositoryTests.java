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

import com.sep.vox.config.TestContainerConfig;
import com.sep.vox.domain.model.question.Question;
import com.sep.vox.domain.model.question.QuestionBank;
import com.sep.vox.domain.model.question.QuestionBankOwnerType;
import com.sep.vox.domain.model.question.QuestionBankStatus;
import com.sep.vox.domain.model.question.QuestionEvaluationGuide;
import com.sep.vox.domain.model.question.QuestionScope;
import com.sep.vox.domain.model.question.QuestionStatus;
import com.sep.vox.domain.model.question.QuestionTopic;
import com.sep.vox.domain.model.question.QuestionTopicStatus;
import com.sep.vox.domain.model.question.QuestionType;
import com.sep.vox.domain.model.question.QuestionVisibility;
import com.sep.vox.domain.repository.QuestionBankRepository;
import com.sep.vox.domain.repository.QuestionEvaluationGuideRepository;
import com.sep.vox.domain.repository.QuestionRepository;
import com.sep.vox.domain.repository.QuestionTopicRepository;
import com.sep.vox.infrastructure.persistence.adapter.QuestionBankRepositoryImpl;
import com.sep.vox.infrastructure.persistence.adapter.QuestionEvaluationGuideRepositoryImpl;
import com.sep.vox.infrastructure.persistence.adapter.QuestionRepositoryImpl;
import com.sep.vox.infrastructure.persistence.adapter.QuestionTopicRepositoryImpl;

@DataJpaTest
@ActiveProfiles("test")
@Import({ TestContainerConfig.class, QuestionBankRepositoryImpl.class, QuestionTopicRepositoryImpl.class, QuestionRepositoryImpl.class, QuestionEvaluationGuideRepositoryImpl.class })
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class QuestionEvaluationGuideRepositoryTests {

    @Autowired
    private QuestionBankRepository questionBankRepository;
    @Autowired
    private QuestionTopicRepository questionTopicRepository;
    @Autowired
    private QuestionRepository questionRepository;
    @Autowired
    private QuestionEvaluationGuideRepository guideRepository;

    @Test
    void when_save_and_findByQuestionId_then_returns_guide() {
        var question = persistQuestion();
        guideRepository.save(new QuestionEvaluationGuide(
            question.getId(), "Expected", "Key points", "Acceptable", "Off topic", "Hints", "Mistakes"
        ));

        var found = guideRepository.findByQuestionId(question.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getExpectedContent()).isEqualTo("Expected");
    }

    private Question persistQuestion() {
        var now = OffsetDateTime.now();
        var bank = questionBankRepository.save(new QuestionBank(UUID.randomUUID(), null, "BANK_GUIDE", "Bank", null,
            QuestionBankOwnerType.SYSTEM, QuestionBankStatus.DRAFT, now, now, UUID.randomUUID(), UUID.randomUUID()));
        var topic = questionTopicRepository.save(new QuestionTopic(bank.getId(), "TOPIC_GUIDE", "Topic", null,
            QuestionTopicStatus.DRAFT, now, now, UUID.randomUUID(), UUID.randomUUID()));
        return questionRepository.save(new Question(topic.getId(), "Q_GUIDE", "Instruction", "Question", "Prompt",
            "Preparation", QuestionType.SHORT_ANSWER, 10, 20, 30, QuestionScope.QUESTION_BANK, QuestionVisibility.BANK_VISIBLE,
            null, false, QuestionStatus.DRAFT, now, now, UUID.randomUUID(), UUID.randomUUID()));
    }
}
