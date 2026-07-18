package com.sep.vox.domain.repository;

import java.util.UUID;

import com.sep.vox.domain.model.exam.ExamItemResponse;

public record ExamRecordingEntry(ExamItemResponse response, UUID examId) {
}
