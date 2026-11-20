package com.sep.vox.application.response.input.dashboard;

/**
 * Một cột trên biểu đồ sức khỏe chấm AI.
 *
 * <p>{@code day} là ngày lịch theo giờ Việt Nam, định dạng {@code yyyy-MM-dd} — cố ý KHÔNG trả
 * instant: client chỉ dùng giá trị này làm nhãn trục và khóa gộp, mà một instant thì client phải quy
 * đổi lại về ngày, và sẽ quy đổi theo múi giờ của trình duyệt chứ không phải múi giờ nghiệp vụ.
 */
public record GradingOutcomeBucketResponse(
    String day,
    long graded,
    long failed
) {
}
