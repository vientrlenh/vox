package com.sep.vox.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.sep.vox.config.ContainerTestConfig;
import com.sep.vox.domain.model.question.Question;
import com.sep.vox.domain.model.question.QuestionAsset;
import com.sep.vox.domain.model.question.QuestionAssetType;
import com.sep.vox.domain.model.question.QuestionBank;
import com.sep.vox.domain.model.question.QuestionBankOwnerType;
import com.sep.vox.domain.model.question.QuestionBankStatus;
import com.sep.vox.domain.model.question.QuestionScope;
import com.sep.vox.domain.model.question.QuestionStatus;
import com.sep.vox.domain.model.question.QuestionTopic;
import com.sep.vox.domain.model.question.QuestionTopicStatus;
import com.sep.vox.domain.model.question.QuestionType;
import com.sep.vox.domain.model.question.QuestionVisibility;
import com.sep.vox.domain.repository.QuestionAssetRepository;
import com.sep.vox.domain.repository.QuestionBankRepository;
import com.sep.vox.domain.repository.QuestionRepository;
import com.sep.vox.domain.repository.QuestionTopicRepository;
import com.sep.vox.infrastructure.persistence.adapter.QuestionAssetRepositoryImpl;
import com.sep.vox.infrastructure.persistence.adapter.QuestionBankRepositoryImpl;
import com.sep.vox.infrastructure.persistence.adapter.QuestionRepositoryImpl;
import com.sep.vox.infrastructure.persistence.adapter.QuestionTopicRepositoryImpl;

@DataJpaTest
@ActiveProfiles("test")
@Import({ QuestionBankRepositoryImpl.class, QuestionTopicRepositoryImpl.class, QuestionRepositoryImpl.class, QuestionAssetRepositoryImpl.class })
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class QuestionAssetRepositoryTests extends ContainerTestConfig {

    @Autowired
    private QuestionBankRepository questionBankRepository;
    @Autowired
    private QuestionTopicRepository questionTopicRepository;
    @Autowired
    private QuestionRepository questionRepository;
    @Autowired
    private QuestionAssetRepository questionAssetRepository;

    @Test
    void when_saveAll_and_findByQuestionId_then_returns_assets_in_order() {
        var question = persistQuestion();
        questionAssetRepository.saveAll(List.of(
            new QuestionAsset(question.getId(), "Audio", 20, null, QuestionAssetType.AUDIO, "audio.mp3", null, null, 2),
            new QuestionAsset(question.getId(), "Image", null, "alt", QuestionAssetType.IMAGE, "image.jpg", null, null, 1)
        ));

        var found = questionAssetRepository.findByQuestionId(question.getId());

        assertThat(found).hasSize(2);
        assertThat(found.get(0).getOrder()).isEqualTo(1);
        assertThat(found.get(1).getOrder()).isEqualTo(2);
    }

    private Question persistQuestion() {
        var now = OffsetDateTime.now();
        var bank = questionBankRepository.save(new QuestionBank(UUID.randomUUID(), null, "BANK_ASSET", "Bank", null,
            QuestionBankOwnerType.SYSTEM, QuestionBankStatus.DRAFT, now, now, UUID.randomUUID(), UUID.randomUUID()));
        var topic = questionTopicRepository.save(new QuestionTopic(bank.getId(), "TOPIC_ASSET", "Topic", null,
            QuestionTopicStatus.DRAFT, now, now, UUID.randomUUID(), UUID.randomUUID()));
        return questionRepository.save(new Question(topic.getId(), "Q_ASSET", "Instruction", "Question", "Prompt",
            "Preparation", QuestionType.SHORT_ANSWER, 10, 20, 30, QuestionScope.QUESTION_BANK, QuestionVisibility.BANK_VISIBLE,
            null, false, QuestionStatus.DRAFT, now, now, UUID.randomUUID(), UUID.randomUUID()));
    }
}
