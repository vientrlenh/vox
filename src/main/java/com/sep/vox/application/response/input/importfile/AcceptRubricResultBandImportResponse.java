package com.sep.vox.application.response.input.importfile;

import java.util.UUID;

public record AcceptRubricResultBandImportResponse(
        UUID sessionId,         // ID của phiên làm việc vừa được đẩy vào hàng đợi ngầm
        long totalRows,         // Tổng số dòng dữ liệu (Để UI hiển thị thanh tiến trình nếu cần)
        long importedRows,      // Số dòng import thành công (Mặc định khi QUEUED là 0)
        long invalidRows,       // Số dòng bị lỗi (Mặc định khi QUEUED là 0)
        long skippedRows,       // Số dòng bị bỏ qua (Mặc định khi QUEUED là 0)
        String status           // Trạng thái hiện tại của phiên làm việc (Sẽ là "QUEUED")
) {
}