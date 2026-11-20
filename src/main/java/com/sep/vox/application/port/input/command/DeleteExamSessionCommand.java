package com.sep.vox.application.port.input.command;

import java.util.UUID;

/**
 * Xoá mềm một phiên thi.
 *
 * <p>{@code reason} là BẮT BUỘC (use case tự kiểm, không để rỗng): xoá một bài thi là thao tác phải
 * giải trình được với học sinh và phụ huynh, nên lý do được lưu cùng dòng dữ liệu chứ không chỉ nằm
 * trong log.
 */
public record DeleteExamSessionCommand(
    UUID sessionId,
    String reason
) {
}
