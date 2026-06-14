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
import com.sep.vox.domain.common.PageRequest;
import com.sep.vox.domain.model.question.QuestionBank;
import com.sep.vox.domain.model.question.QuestionBankOwnerType;
import com.sep.vox.domain.model.question.QuestionBankStatus;
import com.sep.vox.domain.repository.QuestionBankRepository;
import com.sep.vox.infrastructure.persistence.adapter.QuestionBankRepositoryImpl;

@DataJpaTest
@ActiveProfiles("test")
@Import({ TestContainerConfig.class, QuestionBankRepositoryImpl.class })
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class QuestionBankRepositoryTests {

    @Autowired
    private QuestionBankRepository questionBankRepository;

    @Test
    void when_save_and_findById_then_returns_persisted_bank() {
        var saved = questionBankRepository.save(questionBank("QB_01"));

        var found = questionBankRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getCode()).isEqualTo("QB_01");
    }

    @Test
    void when_findAll_then_returns_paginated_banks() {
        questionBankRepository.save(questionBank("QB_02"));
        questionBankRepository.save(questionBank("QB_03"));

        var page = questionBankRepository.findAll(new PageRequest(1, 10));

        assertThat(page.content()).hasSizeGreaterThanOrEqualTo(2);
    }

    private QuestionBank questionBank(String code) {
        var now = OffsetDateTime.now();
        return new QuestionBank(
            UUID.randomUUID(),
            null,
            code,
            "Bank " + code,
            "Description",
            QuestionBankOwnerType.SYSTEM,
            QuestionBankStatus.DRAFT,
            now,
            now,
            UUID.randomUUID(),
            UUID.randomUUID()
        );
    }
}
