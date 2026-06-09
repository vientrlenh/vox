package com.sep.vox.application.query.repository;

import java.util.UUID;

import com.sep.vox.domain.model.question.QuestionBankStatus;

public interface QuestionBankPermissionQuery {
    boolean canUpdateBank(UUID bankId);
    boolean canPublishBank(UUID bankId);
    boolean canArchiveBank(UUID bankId);
    boolean canRestoreBank(UUID bankId);
}
