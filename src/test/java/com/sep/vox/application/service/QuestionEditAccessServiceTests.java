package com.sep.vox.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.service.QuestionEditAccessService;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.question.Question;
import com.sep.vox.domain.model.question.QuestionAsset;
import com.sep.vox.domain.model.question.QuestionBank;
import com.sep.vox.domain.model.question.QuestionBankOwnerType;
import com.sep.vox.domain.model.question.QuestionStatus;
import com.sep.vox.domain.repository.QuestionAssetRepository;
import com.sep.vox.domain.repository.QuestionBankRepository;
import com.sep.vox.domain.repository.QuestionCollaboratorRepository;
import com.sep.vox.domain.repository.QuestionRepository;

@ExtendWith(MockitoExtension.class)
class QuestionEditAccessServiceTests {

    private static final UUID OWNER_ID = UUID.randomUUID();
    private static final UUID OTHER_USER_ID = UUID.randomUUID();
    private static final UUID BANK_ID = UUID.randomUUID();
    private static final UUID QUESTION_ID = UUID.randomUUID();

    @Mock
    private QuestionRepository questionRepository;
    @Mock
    private QuestionAssetRepository questionAssetRepository;
    @Mock
    private QuestionBankRepository questionBankRepository;
    @Mock
    private QuestionCollaboratorRepository questionCollaboratorRepository;
    @Mock
    private UserContextPort userContextPort;

    @InjectMocks
    private QuestionEditAccessService service;

    @Test
    void authorizeEditingReturnsFalseForOwnerOfDraftQuestion() {
        Question question = question(QuestionStatus.DRAFT, false);
        givenBank(QuestionBankOwnerType.SCHOOL);
        when(questionRepository.existsUsedInAssessment(QUESTION_ID)).thenReturn(false);

        assertThat(service.authorizeEditing(question, OWNER_ID)).isFalse();
    }

    @Test
    void authorizeEditingReturnsTrueWhenQuestionIsPublished() {
        Question question = question(QuestionStatus.PUBLISHED, false);
        givenBank(QuestionBankOwnerType.SCHOOL);

        assertThat(service.authorizeEditing(question, OWNER_ID)).isTrue();
    }

    @Test
    void authorizeEditingReturnsTrueWhenQuestionIsUsedInAssessment() {
        Question question = question(QuestionStatus.DRAFT, false);
        givenBank(QuestionBankOwnerType.SCHOOL);
        when(questionRepository.existsUsedInAssessment(QUESTION_ID)).thenReturn(true);

        assertThat(service.authorizeEditing(question, OWNER_ID)).isTrue();
    }

    @Test
    void authorizeEditingRejectsOwnerWhenStatusIsNotEditable() {
        Question question = question(QuestionStatus.ARCHIVED, false);
        givenBank(QuestionBankOwnerType.SCHOOL);
        when(questionRepository.existsUsedInAssessment(QUESTION_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.authorizeEditing(question, OWNER_ID))
            .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void authorizeEditingRejectsUserWithoutAnyAccess() {
        Question question = question(QuestionStatus.DRAFT, false);
        givenBank(QuestionBankOwnerType.SCHOOL);
        when(questionCollaboratorRepository.findByQuestionIdAndUserId(QUESTION_ID, OTHER_USER_ID))
            .thenReturn(Optional.empty());
        when(userContextPort.isSystemAdmin()).thenReturn(false);

        assertThatThrownBy(() -> service.authorizeEditing(question, OTHER_USER_ID))
            .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void authorizeEditingAllowsSystemAdminOnSystemBankRegardlessOfStatus() {
        Question question = question(QuestionStatus.ARCHIVED, false);
        givenBank(QuestionBankOwnerType.SYSTEM);
        when(questionCollaboratorRepository.findByQuestionIdAndUserId(QUESTION_ID, OTHER_USER_ID))
            .thenReturn(Optional.empty());
        when(userContextPort.isSystemAdmin()).thenReturn(true);
        when(questionRepository.existsUsedInAssessment(QUESTION_ID)).thenReturn(false);

        assertThat(service.authorizeEditing(question, OTHER_USER_ID)).isFalse();
    }

    @Test
    void authorizeRegenerationIgnoresQuestionStatusForOwner() {
        Question question = question(QuestionStatus.ARCHIVED, false);
        givenBank(QuestionBankOwnerType.SCHOOL);
        when(questionRepository.existsUsedInAssessment(QUESTION_ID)).thenReturn(false);

        assertThat(service.authorizeRegeneration(question, OWNER_ID)).isFalse();
    }

    @Test
    void requireAssetOfRejectsAssetOfAnotherQuestion() {
        UUID assetId = UUID.randomUUID();
        Question question = question(QuestionStatus.DRAFT, false);
        QuestionAsset foreignAsset = QuestionAsset.builder().id(assetId).questionId(UUID.randomUUID()).build();
        when(questionAssetRepository.findById(assetId)).thenReturn(Optional.of(foreignAsset));

        assertThatThrownBy(() -> service.requireAssetOf(question, assetId))
            .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void requireAssetOfRejectsMissingAsset() {
        UUID assetId = UUID.randomUUID();
        Question question = question(QuestionStatus.DRAFT, false);
        when(questionAssetRepository.findById(assetId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.requireAssetOf(question, assetId))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void requireAssetOfReturnsAssetOfTheQuestion() {
        UUID assetId = UUID.randomUUID();
        Question question = question(QuestionStatus.DRAFT, false);
        QuestionAsset asset = QuestionAsset.builder().id(assetId).questionId(QUESTION_ID).build();
        when(questionAssetRepository.findById(assetId)).thenReturn(Optional.of(asset));

        assertThat(service.requireAssetOf(question, assetId)).isSameAs(asset);
        verify(questionAssetRepository).findById(assetId);
    }

    private Question question(QuestionStatus status, boolean locked) {
        return Question.builder()
            .id(QUESTION_ID)
            .questionBankId(BANK_ID)
            .status(status)
            .locked(locked)
            .createdBy(OWNER_ID.toString())
            .build();
    }

    private void givenBank(QuestionBankOwnerType ownerType) {
        QuestionBank bank = QuestionBank.builder().id(BANK_ID).ownerType(ownerType).build();
        when(questionBankRepository.findById(BANK_ID)).thenReturn(Optional.of(bank));
    }
}
