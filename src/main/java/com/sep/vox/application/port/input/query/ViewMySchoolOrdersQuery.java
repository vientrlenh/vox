package com.sep.vox.application.port.input.query;

/**
 * Không mang schoolId: trường lấy từ token của school admin đang đăng nhập, cùng lý do với
 * {@link com.sep.vox.application.port.input.command.CreateSubscriptionOrderCommand}.
 */
public record ViewMySchoolOrdersQuery(
    int page,
    int size
) {
}
