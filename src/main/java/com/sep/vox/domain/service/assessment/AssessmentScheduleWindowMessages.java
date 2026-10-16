package com.sep.vox.domain.service.assessment;

import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentKind;

/**
 * Thông báo lỗi dùng chung cho các ràng buộc khung giờ ca thi, để mọi nơi áp cùng một bất biến
 * (tạo/sửa ca thi, sửa kỳ thi, tính lại thời gian làm bài) báo cùng một cách.
 */
public final class AssessmentScheduleWindowMessages {

    private AssessmentScheduleWindowMessages() {
    }

    public static String tooShortForAssessmentTime(Assessment assessment) {
        return "Thời lượng ca thi phải lớn hơn hoặc bằng thời gian làm bài của kỳ thi ("
            + formatDuration(assessment.getTimeDurationSecond()) + ")";
    }

    /** Hiển thị thời lượng dạng "xx phút yy giây" thay vì giây thô cho dễ đọc. */
    private static String formatDuration(Integer totalSeconds) {
        if (totalSeconds == null) {
            return "0 giây";
        }
        var minutes = totalSeconds / 60;
        var seconds = totalSeconds % 60;
        if (minutes == 0) {
            return seconds + " giây";
        }
        return seconds == 0 ? minutes + " phút" : minutes + " phút " + seconds + " giây";
    }

    public static String outsideAssessmentWindow(Assessment assessment) {
        var message = new StringBuilder("Ca thi phải nằm trong khoảng thời gian mở và đóng của kỳ thi");
        if (assessment.getOpenAt() != null) {
            message.append(", mở lúc ").append(assessment.getOpenAt());
        }
        if (assessment.getCloseAt() != null) {
            message.append(", đóng lúc ").append(assessment.getCloseAt());
        }
        return message.toString();
    }

    /**
     * Ca thi đã lên lịch không còn đủ dài sau khi kỳ thi đổi khung giờ hoặc thời gian làm bài.
     * Với CLASS_TEST, ca thi chính là khung openAt/closeAt nên phải chỉ vào thời gian đóng bài --
     * bảo người dùng "sửa ca thi" là vô nghĩa, nhất là khi lỗi nổ ngay lúc tạo bài kiểm tra.
     */
    public static String schedulesNoLongerFit(int count, Assessment assessment) {
        if (assessment.getKind() == AssessmentKind.CLASS_TEST) {
            return "Tổng thời gian làm bài (" + formatDuration(assessment.getTimeDurationSecond())
                + ") vượt quá khoảng mở và đóng bài đã đặt. Vui lòng kéo dài thời gian đóng bài.";
        }
        return "Thời gian làm bài (" + formatDuration(assessment.getTimeDurationSecond()) + ") vượt quá thời lượng của "
            + count + " ca thi đã lên lịch. Vui lòng kéo dài các ca thi này trước.";
    }

    public static String schedulesOutsideNewWindow(int count) {
        return "Có " + count + " ca thi nằm ngoài khoảng thời gian mở và đóng mới của kỳ thi. "
            + "Vui lòng dời các ca thi này trước.";
    }
}
