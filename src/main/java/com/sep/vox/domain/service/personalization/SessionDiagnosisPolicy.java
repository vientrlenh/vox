package com.sep.vox.domain.service.personalization;

public final class SessionDiagnosisPolicy {

    private SessionDiagnosisPolicy() {
    }

    /**
     * Suy luận lý do học sinh bỏ dở phiên luyện tập, dựa trên điểm gần nhất và tín hiệu
     * xin trợ giúp/tạm dừng lâu trong phiên.
     *
     * <p><b>Hai tham số hành vi giờ mới thật sự có dữ liệu.</b> Trước đây client gửi cứng
     * {@code 0} cho cả hai, nên luật này thoái hoá thành một ngưỡng điểm thuần: vế
     * {@code helpRequestCount == 0 && longPauseCount <= 1} luôn đúng và vế
     * {@code helpRequestCount >= 1 || longPauseCount >= 2} luôn sai, tức "chán" bị suy ra từ
     * ĐIỂM CAO chứ không từ hành vi. Nguồn hiện tại:
     *
     * <ul>
     *   <li>{@code helpRequestCount} — số lần học sinh bấm nút "Gợi ý" trong phiên. Tín hiệu
     *       chủ động: chính em nói rằng em bí.</li>
     *   <li>{@code longPauseCount} — số lượt mà sau khi AI đọc xong câu hỏi, VAD không báo
     *       {@code vad_speech_start} nào trong 10 giây. Đếm tối đa một lần mỗi lượt.</li>
     * </ul>
     *
     * <p>Chỉ gọi ở đường học sinh TỰ bấm kết thúc. Đường dọn phiên rớt mạng
     * ({@code PracticeSessionCleanupService}) không có hai con số này nên trả thẳng
     * {@code UNKNOWN} thay vì đoán -- xem chú thích ở đó.
     *
     * <p><b>Kết quả đi đâu:</b> {@code practice_session.abandon_diagnosis}, và nơi tiêu thụ
     * DUY NHẤT có tác dụng là cổng vào của
     * {@code InterestVectorService.recordSessionOutcome} -- phiên không hoàn thành chỉ được
     * ghi tín hiệu sở thích khi chẩn đoán là {@code BORED}. {@code TOO_HARD} cố ý KHÔNG ghi
     * gì: bỏ vì khó không có nghĩa là không thích.
     */
    public static String diagnose(
            Double normalizedScore,
            int helpRequestCount,
            int longPauseCount) {
        if (normalizedScore == null) {
            // Đây là đường CHÍNH, không phải ngoại lệ hiếm.
            //
            // Phương thức này chỉ được gọi khi status = ABANDONED, mà ABANDONED <=>
            // gradedSeconds == 0 <=> học sinh chưa nói lượt nào <=> chưa có bản chấm nào <=>
            // normalizedScore == null. Nói cách khác: nhánh này chạy MỌI lần, còn hai luật
            // theo điểm bên dưới thì không bao giờ. Trả thẳng UNKNOWN như trước đồng nghĩa
            // với việc abandon_diagnosis vĩnh viễn là UNKNOWN, BORED không bao giờ xuất hiện,
            // và vì InterestVectorService chỉ ghi tín hiệu khi completed HOẶC BORED nên phiên
            // bỏ dở KHÔNG BAO GIỜ dạy được gì cho hồ sơ sở thích. Kiểm trên DB 2026-08-05:
            // toàn hệ 0 phiên BORED, 0 phiên TOO_HARD.
            //
            // Không có điểm nhưng VẪN CÒN hành vi -- và hai con số này đo được kể cả khi học
            // sinh chưa mở miệng, vì longPauseCount đếm sau lúc AI đọc xong câu hỏi.
            if (helpRequestCount >= 1 || longPauseCount >= 2) {
                // Đã thử: bấm xin gợi ý, hoặc ngồi im hết 10 giây qua hai câu.
                return "TOO_HARD";
            }
            if (longPauseCount == 0) {
                // Thoát trước cả khi im đủ 10 giây một lần: không có dấu hiệu bí nào, mà vẫn
                // không muốn tiếp. Cùng dạng lập luận với luật điểm cao bên dưới -- "không
                // vất vả mà vẫn bỏ".
                //
                // Chấp nhận rủi ro dương giả (có việc đột xuất, bấm nhầm): tín hiệu chỉ 0,15
                // và EMA α = 0,3 nên một lần sai kéo điểm từ 0,50 xuống 0,395, vài buổi sau
                // là hồi. Đổi lại, đây là đường DUY NHẤT để phiên bỏ dở nói được gì.
                return "BORED";
            }
            // Đúng một lần im 10 giây rồi thoát: một điểm dữ liệu, hai cách giải thích ngang
            // nhau. Không đoán.
            return "UNKNOWN";
        }
        if (normalizedScore >= 0.65
                && helpRequestCount == 0
                && longPauseCount <= 1) {
            return "BORED";
        }
        if (normalizedScore < 0.50
                || helpRequestCount >= 1
                || longPauseCount >= 2) {
            return "TOO_HARD";
        }
        return "UNKNOWN";
    }
}
