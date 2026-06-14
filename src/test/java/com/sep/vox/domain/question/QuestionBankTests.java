package com.sep.vox.domain.question;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.sep.vox.domain.model.question.QuestionBank;
import com.sep.vox.domain.model.question.QuestionBankOwnerType;
import com.sep.vox.domain.model.question.QuestionBankStatus;

class QuestionBankTests {

    @Test
    void create_should_initialize_draft_timestamps_and_audit_fields() {
        var now = OffsetDateTime.now();
        var languageId = UUID.randomUUID();
        var schoolId = UUID.randomUUID();
        var createdBy = UUID.randomUUID();

        var bank = QuestionBank.create(
            languageId,
            schoolId,
            "BANK_01",
            "Speaking Bank",
            "Question bank",
            QuestionBankOwnerType.SCHOOL,
            now,
            createdBy
        );

        assertThat(bank.getLanguageId()).isEqualTo(languageId);
        assertThat(bank.getSchoolId()).isEqualTo(schoolId);
        assertThat(bank.getCode()).isEqualTo("BANK_01");
        assertThat(bank.getName()).isEqualTo("Speaking Bank");
        assertThat(bank.getDescription()).isEqualTo("Question bank");
        assertThat(bank.getOwnerType()).isEqualTo(QuestionBankOwnerType.SCHOOL);
        assertThat(bank.getStatus()).isEqualTo(QuestionBankStatus.DRAFT);
        assertThat(bank.getCreatedAt()).isEqualTo(now);
        assertThat(bank.getUpdatedAt()).isEqualTo(now);
        assertThat(bank.getCreatedBy()).isEqualTo(createdBy);
        assertThat(bank.getUpdatedBy()).isEqualTo(createdBy);
    }
}
