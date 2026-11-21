package com.sep.vox.application.usecase.question;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.exception.DataConflictException;
import com.sep.vox.application.port.input.service.ImportCommitResult;
import com.sep.vox.application.port.input.service.QuestionImportCommitHandler;
import com.sep.vox.application.port.output.RequiresNewTransactionPort;
import com.sep.vox.application.support.FakeJsonSerializationPort;
import com.sep.vox.domain.model.importfile.ImportRow;
import com.sep.vox.domain.model.importfile.ImportRowStatus;
import com.sep.vox.domain.model.importfile.ImportSession;
import com.sep.vox.domain.model.importfile.ImportType;
import com.sep.vox.domain.model.question.QuestionBank;
import com.sep.vox.domain.model.question.QuestionTopic;
import com.sep.vox.domain.model.question.QuestionTopicStatus;
import com.sep.vox.domain.repository.QuestionAssetRepository;
import com.sep.vox.domain.repository.QuestionBankRepository;
import com.sep.vox.domain.repository.QuestionEvaluationGuideRepository;
import com.sep.vox.domain.repository.QuestionRepository;
import com.sep.vox.domain.repository.QuestionTopicRepository;
import com.sep.vox.domain.repository.SchoolRepository;

class QuestionImportCommitHandlerTests {

    private final UUID questionBankId = UUID.randomUUID();
    private final UUID questionTopicId = UUID.randomUUID();

    private QuestionBankRepository questionBankRepository;
    private QuestionTopicRepository questionTopicRepository;
    private RequiresNewTransactionPort requiresNewTransactionPort;
    private FakeJsonSerializationPort jsonSerializationPort;
    private QuestionImportCommitHandler handler;

    @BeforeEach
    void setUp() {
        questionBankRepository = mock(QuestionBankRepository.class);
        questionTopicRepository = mock(QuestionTopicRepository.class);
        requiresNewTransactionPort = mock(RequiresNewTransactionPort.class);
        jsonSerializationPort = new FakeJsonSerializationPort();
        handler = new QuestionImportCommitHandler(
            mock(QuestionRepository.class), questionBankRepository, questionTopicRepository,
            mock(QuestionEvaluationGuideRepository.class), mock(QuestionAssetRepository.class),
            mock(SchoolRepository.class), jsonSerializationPort, requiresNewTransactionPort);
    }

    @Test
    void should_mark_failed_when_the_transaction_reports_a_data_conflict() {
        when(questionBankRepository.findById(questionBankId)).thenReturn(Optional.of(QuestionBank.builder().id(questionBankId).build()));
        when(questionTopicRepository.findById(questionTopicId)).thenReturn(Optional.of(QuestionTopic.builder()
            .id(questionTopicId).questionBankId(questionBankId).status(QuestionTopicStatus.PUBLISHED).build()));
        doThrow(new DataConflictException("unique violation", null)).when(requiresNewTransactionPort).runInNewTransaction(any());
        ImportRow row = row();

        ImportCommitResult result = handler.commit(session(), List.of(row));

        assertThat(result.created()).isZero();
        assertThat(result.invalid()).isEqualTo(1L);
        assertThat(row.getStatus()).isEqualTo(ImportRowStatus.FAILED);
        assertThat(row.getErrorsJson()).contains("Không thể tạo câu hỏi từ dữ liệu import");
    }

    private ImportRow row() {
        Map<String, String> rawData = Map.of(
            "questionBankId", questionBankId.toString(),
            "questionTopicId", questionTopicId.toString(),
            "type", "SHORT_ANSWER",
            "questionText", "Describe your hometown.",
            "preparationTimeSeconds", "10",
            "minResponseSeconds", "5",
            "maxResponseSeconds", "60");
        return ImportRow.builder()
            .id(UUID.randomUUID())
            .sessionId(UUID.randomUUID())
            .rowNumber(1L)
            .rawDataJson(jsonSerializationPort.toJson(rawData))
            .status(ImportRowStatus.PENDING)
            .build();
    }

    private ImportSession session() {
        Map<String, String> mapping = Map.of(
            "questionBankId", "questionBankId",
            "questionTopicId", "questionTopicId",
            "type", "type",
            "questionText", "questionText",
            "preparationTimeSeconds", "preparationTimeSeconds",
            "minResponseSeconds", "minResponseSeconds",
            "maxResponseSeconds", "maxResponseSeconds");
        return ImportSession.builder()
            .id(UUID.randomUUID())
            .type(ImportType.QUESTION)
            .confirmedMappingJson(jsonSerializationPort.toJson(mapping))
            .createdBy(UUID.randomUUID().toString())
            .build();
    }
}
