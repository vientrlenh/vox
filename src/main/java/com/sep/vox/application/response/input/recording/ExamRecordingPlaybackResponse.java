package com.sep.vox.application.response.input.recording;

import java.util.UUID;

/**
 * Một bản ghi buổi thi kèm link phát, cho màn chấm bài.
 *
 * <p>Cố tình KHÔNG chở {@code bucket} và {@code s3Key}: chúng là chi tiết lưu trữ, và phơi ra
 * ngoài thì client sẽ tự ghép URL -- đúng thứ khiến link mất hạn và mất kiểm soát quyền.
 */
public record ExamRecordingPlaybackResponse(
    UUID id,
    String streamType,
    String status,
    String source,
    Integer durationSeconds,
    boolean canonical,
    String playbackUrl
) {
}
