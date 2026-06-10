package com.sep.vox.application.query.repository;

import java.util.UUID;

public interface QuestionViewPermissionQuery {
    boolean canViewQuestionDetail(UUID questionId);
}
