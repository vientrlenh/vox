package com.sep.vox.application.port.input.command;

import com.sep.vox.application.common.UploadedFile;

/**
 * Import ngân hàng câu hỏi.
 *
 * <p>Không có tham số phạm vi: ngân hàng thuộc trường nào (hay thuộc hệ thống) suy từ vai trò của
 * người đang đăng nhập ở use case. Cho client tự khai schoolId là mở đường để một người tạo ngân
 * hàng cho trường khác.
 */
public record PreviewQuestionBankImportFromFileCommand(
    UploadedFile file
) {
}
