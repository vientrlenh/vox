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
import com.sep.vox.domain.model.question.QuestionBank;
import com.sep.vox.domain.model.question.QuestionBankOwnerType;
import com.sep.vox.domain.model.question.QuestionBankStatus;
import com.sep.vox.domain.model.question.QuestionTopic;
import com.sep.vox.domain.model.question.QuestionTopicStatus;
import com.sep.vox.domain.repository.QuestionBankRepository;
import com.sep.vox.domain.repository.QuestionTopicRepository;
import com.sep.vox.infrastructure.persistence.adapter.QuestionBankRepositoryImpl;
import com.sep.vox.infrastructure.persistence.adapter.QuestionTopicRepositoryImpl;

@DataJpaTest
@ActiveProfiles("test")
@Import({ QuestionBankRepositoryImpl.class, QuestionTopicRepositoryImpl.class })
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class QuestionTopicRepositoryTests extends ContainerTestConfig {

    @Autowired
    private QuestionBankRepository questionBankRepository;

    @Autowired
    private QuestionTopicRepository questionTopicRepository;

    @Test
    void when_save_and_findByQuestionBankId_then_returns_topics() {
        var bank = questionBankRepository.save(questionBank("BANK_TOPIC"));
        questionTopicRepository.save(topic(bank.getId(), "TOPIC_01"));

        var found = questionTopicRepository.findByQuestionBankId(bank.getId());

        assertThat(found).hasSize(1);
        assertThat(found.get(0).getCode()).isEqualTo("TOPIC_01");
    }

    @Test
    void when_findByQuestionBankId_with_page_then_returns_page() {
        var bank = questionBankRepository.save(questionBank("BANK_TOPIC_PAGE"));
        questionTopicRepository.save(topic(bank.getId(), "TOPIC_02"));

        var page = questionTopicRepository.findByQuestionBankId(bank.getId(), new PageRequest(1, 10));

        assertThat(page.content()).hasSize(1);
    }

    private QuestionBank questionBank(String code) {
        var now = OffsetDateTime.now();
        return new QuestionBank(UUID.randomUUID(), null, code, code, null, QuestionBankOwnerType.SYSTEM,
            QuestionBankStatus.DRAFT, now, now, UUID.randomUUID(), UUID.randomUUID());
    }

    private QuestionTopic topic(UUID bankId, String code) {
        var now = OffsetDateTime.now();
        return new QuestionTopic(bankId, code, code, null, QuestionTopicStatus.DRAFT, now, now, UUID.randomUUID(), UUID.randomUUID());
    }
}
