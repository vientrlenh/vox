package com.sep.vox.application.projection.repository;

import java.util.UUID;

import com.sep.vox.application.projection.dto.QuestionBankStatsDto;

public interface QuestionBankStatsQueryRepository {
    QuestionBankStatsDto countForSchool(UUID schoolId);
}
