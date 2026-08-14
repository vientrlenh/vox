package com.sep.vox.application.port.output;

/**
 * Kiểm tra dịch vụ AI có sẵn sàng nhận phiên luyện nói hay không. Implement bởi
 * PracticeAgentAvailabilityClient.
 */
public interface PracticeAgentAvailabilityPort {

    /**
     * Ném IllegalStateException nếu dịch vụ AI chưa sẵn sàng -- KHÔNG suy giảm êm như các port AI
     * khác, vì vào phiên mà agent chưa sống thì học sinh ngồi chờ vô ích, thà chặn ngay từ đầu.
     */
    void requireReady();
}
