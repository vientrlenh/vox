package com.sep.vox.domain.repository.personalization;

import java.util.UUID;

/**
 * Một câu đã trả lời nhưng chưa có bản chấm -- diện cần xả chấm lúc đóng phiên.
 *
 * Cần CẢ HAI id: {@code responseId} để gom các lượt đã nói, {@code questionId} để lấy đề bài
 * và hướng dẫn chấm. Trả về mỗi questionId rồi upsert lại để lấy responseId là lách -- upsert
 * còn nối thêm transcript và ghi đè audioUrl, tức là sửa dữ liệu chỉ để đọc một cái id.
 */
public interface PendingEvaluationResponse {

    UUID getResponseId();

    UUID getQuestionId();
}
