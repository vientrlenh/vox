package com.sep.vox.application.query.repository;

import java.util.UUID;

import com.sep.vox.domain.model.question.QuestionStatus;

public interface QuestionPermissionQuery {

    boolean canEditContent(UUID questionId);

    boolean canReview(UUID questionId, QuestionStatus targetStatus);
}
