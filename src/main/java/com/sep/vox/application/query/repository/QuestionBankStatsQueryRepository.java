package com.sep.vox.application.query.repository;

import java.util.UUID;

import com.sep.vox.application.query.dto.QuestionBankStatsDto;

public interface QuestionBankStatsQueryRepository {
    QuestionBankStatsDto countForSchool(UUID schoolId);
}
