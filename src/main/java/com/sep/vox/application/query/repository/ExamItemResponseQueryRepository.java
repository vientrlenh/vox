package com.sep.vox.application.query.repository;

import java.util.List;
import java.util.UUID;

import com.sep.vox.application.query.dto.ExamItemResponseDto;

public interface ExamItemResponseQueryRepository {
    List<ExamItemResponseDto> findByStudentIdWithAudio(UUID studentId);
}
