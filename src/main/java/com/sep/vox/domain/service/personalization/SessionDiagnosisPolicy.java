package com.sep.vox.domain.service.personalization;

public final class SessionDiagnosisPolicy {
    // Hai phần trong cùng một file vì cùng trả lời MỘT câu hỏi: buổi vừa rồi nói lên điều gì
    // về sở thích của học sinh. diagnose() lo phần "vì sao bỏ dở", signal() lo phần "mạnh tới
    // đâu", và cả hai cùng chảy vào InterestVectorService.recordSessionOutcome. Tách ra hai
    // lớp thì mỗi lần chỉnh mức tín hiệu lại phải mở hai file để đối chiếu bảng giá trị.

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
     * <p>Sức mạnh của tín hiệu ấy do {@link #signal} quyết định -- xem javadoc ở đó.
     *
     * <p><b>Kết quả đi đâu:</b> {@code practice_sessions.abandon_diagnosis}, và nơi tiêu thụ
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
            return diagnoseWithoutScore(helpRequestCount, longPauseCount);
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

    /**
     * This is the MAIN path, not a rare exception.
     *
     * <p>{@link #diagnose} is only called when status = ABANDONED, and ABANDONED <=> gradedSeconds == 0
     * <=> the student has not spoken a single turn <=> there is no grading yet <=> normalizedScore ==
     * null. In other words: this branch runs EVERY time, while the two score-based rules in
     * {@code diagnose} never do. Returning UNKNOWN straight away, as before, meant abandon_diagnosis
     * was UNKNOWN forever, BORED never appeared, and since InterestVectorService only records a signal
     * when the session is completed OR BORED, an abandoned session NEVER taught the interest profile
     * anything. Checked on the DB on 2026-08-05: the whole system had 0 BORED sessions and 0 TOO_HARD
     * sessions.
     *
     * <p>There is no score but there IS still behaviour -- and both numbers are measurable even when
     * the student has not opened their mouth, because longPauseCount counts after the AI has finished
     * reading the question.
     *
     * <p>Having tried (asked for a hint, or sat silent for the full 10 seconds across two questions)
     * means TOO_HARD. Leaving before even one full 10-second silence shows no sign of being stuck yet
     * still not wanting to continue; it follows the same reasoning as the high-score rule in
     * {@code diagnose} -- "not struggling yet still quits" -- and means BORED. The false-positive risk
     * (an urgent errand, a mis-tap) is accepted: the signal is only 0.15 and with EMA alpha = 0.3 one
     * wrong call moves the score from 0.50 to 0.395, which recovers within a few sessions. In return
     * this is the ONLY path through which an abandoned session can say anything. Exactly one 10-second
     * silence and then leaving is one data point with two equally likely explanations, so it is not
     * guessed: UNKNOWN.
     */
    private static String diagnoseWithoutScore(int helpRequestCount, int longPauseCount) {
        if (helpRequestCount >= 1 || longPauseCount >= 2) {
            return "TOO_HARD";
        }
        if (longPauseCount == 0) {
            return "BORED";
        }
        return "UNKNOWN";
    }

    /** Từ mốc này trở lên là tín hiệu đầy đủ, y như trước khi có thang. */
    public static final int FULL_SIGNAL_SECONDS = 120;

    /**
     * @param origin nguồn chủ đề vào phiên: học sinh tự tìm ({@code KEYWORD}), hệ thống thăm dò
     *               ({@code EXPLORATION}/{@code RANDOM}/{@code EPSILON}), hay được đề xuất.
     * @param spokenSeconds số giây VAD nghe thấy tiếng -- cùng con số quota trừ, KHÔNG phải
     *                      thời lượng phiên (thời lượng gồm cả lúc AI nói và lúc ngồi nghĩ).
     *                      Bằng 0 ở phiên bỏ dở, và khi đó hàm trả đúng mức cũ.
     */
    public static double signal(String origin, int spokenSeconds) {
        double abandoned = abandonedSignal(origin);
        double full = fullSignal(origin);
        if (spokenSeconds <= 0) {
            return abandoned;
        }
        if (spokenSeconds >= FULL_SIGNAL_SECONDS) {
            return full;
        }
        double progress = (double) spokenSeconds / FULL_SIGNAL_SECONDS;
        return abandoned + progress * (full - abandoned);
    }

    /**
     * Mức đầy đủ theo nguồn -- giữ nguyên bảng cũ.
     *
     * <p>{@code EPSILON} xếp CÙNG NHÓM với {@code EXPLORATION} (sửa 2026-08-06). Trước đó nó
     * không có case nên rơi vào {@code default} và nhận 0,95 -- y hệt học sinh tự chọn, đúng
     * cái "dương giả" mà chú thích ở {@code BuildPracticePaperUseCase.resolveOrigin} tưởng là
     * đã tránh được.
     *
     * <p>Vì sao cùng nhóm: cả hai đều là HỆ THỐNG đưa chủ đề tới, không phải bằng chứng học
     * sinh vốn thích nó. Với ε-greedy thì đây còn là điều kiện để phép đo có nghĩa -- nếu điểm
     * của slot thăm dò bằng điểm của slot khai thác thì chính thứ mà thăm dò sinh ra để đo lại
     * bị nhiễu bởi ưu thế của lựa chọn có sẵn.
     *
     * <p>Chấp nhận đánh đổi: chủ đề chỉ từng vào phiên qua đường thăm dò thì lần đầu tối đa chỉ
     * được 0,60. Muốn lên cao hơn thì lần sau em phải CHỦ ĐỘNG chọn lại -- đúng thứ ta muốn coi
     * là bằng chứng thật.
     */
    private static double fullSignal(String origin) {
        return switch (origin == null ? "" : origin) {
            case "KEYWORD" -> 1.00;
            case "EXPLORATION", "RANDOM", "EPSILON" -> 0.60;
            default -> 0.95;
        };
    }

    /**
     * Mức của phiên bỏ dở -- cũng giữ nguyên bảng cũ, và chỉ tới được khi
     * {@link SessionDiagnosisPolicy} kết luận {@code BORED}.
     *
     * <p>Thăm dò bị phạt NHẸ NHẤT (0,10 so với 0,20 của tự tìm) vì cùng một logic với
     * {@link #fullSignal}: hệ thống tự đưa chủ đề tới thì việc học sinh bỏ ngang nói ít về em
     * ấy hơn là khi chính em gõ từ khoá đi tìm rồi vẫn bỏ.
     */
    private static double abandonedSignal(String origin) {
        return switch (origin == null ? "" : origin) {
            case "KEYWORD" -> 0.20;
            case "EXPLORATION", "RANDOM", "EPSILON" -> 0.10;
            default -> 0.15;
        };
    }
}
