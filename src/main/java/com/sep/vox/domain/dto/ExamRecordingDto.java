package com.sep.vox.domain.dto;

import java.util.UUID;

public record ExamRecordingDto(
    UUID id, 
    UUID examSessionId, 
    UUID candidateId, 
    String streamType, 
    String status, 
    Long sizeBytes,
    Integer durationSeconds,
    String source,
    // Bản được coi là chuẩn cho streamType này. Mỗi nguồn ingest giữ một hàng riêng, nên một
    // phiên thi có thể có nhiều bản ghi cùng loại; cờ này nói bản nào nên mở trước, mà không
    // giấu đi các bản còn lại -- bản WebRTC là bản duy nhất không đi qua máy thí sinh, nên nó
    // phải luôn với tới được kể cả khi không phải bản chuẩn.
    boolean canonical,
    String createdAt,
    String assembledAt
) {
    
}
