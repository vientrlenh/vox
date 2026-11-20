package com.sep.vox.application.port.output;

import java.time.Duration;

import com.sep.vox.application.common.StoredFile;
import com.sep.vox.application.common.UploadedFile;

public interface StoragePort {
    record PresignedUpload(String uploadUrl, String publicUrl) {
    }

    StoredFile store(String key, UploadedFile file);

    void delete(String key);

    String resolveUrl(String key);

    PresignedUpload presignUpload(String key, String contentType, Duration ttl);

    /**
     * Link ĐỌC có hạn cho một object ở bucket CHỈ ĐỊNH -- để phát thẳng trong thẻ
     * {@code <video>}, không phải để tải về.
     *
     * <p>S3 trả link này kèm hỗ trợ HTTP Range, nên trình duyệt phát và TUA được mà không phải
     * tải hết file trước. Đó là lý do chọn hướng này thay vì proxy qua server: bản ghi buổi thi
     * dài hàng chục phút, đẩy qua backend là gánh băng thông vô ích.
     *
     * <p>Khác mọi method còn lại ở hai điểm, và cả hai đều bắt buộc: bucket truyền vào thay vì
     * lấy từ cấu hình, và key dùng nguyên văn không gắn thêm {@code keyPrefix}.
     *
     * <p>Bản ghi buổi thi do vox-streaming ghi thẳng lên bucket riêng của nó
     * ({@code STORAGE_RECORDING_BUCKET}), khác bucket tải lên của ứng dụng
     * ({@code AWS_S3_BUCKET}), và key đầy đủ đã nằm sẵn trong {@code exam_recordings.s3_key}.
     * Dùng lại {@code resolveUrl} sẽ vừa trỏ sai bucket vừa chèn thừa tiền tố {@code dev/}.
     *
     * @param bucket bucket chứa object -- rỗng thì ném, vì đoán bucket là cách chắc chắn nhất
     *               để tạo ra một link 404 mà không ai hiểu vì sao
     */
    String presignRead(String bucket, String key, Duration ttl);
}
