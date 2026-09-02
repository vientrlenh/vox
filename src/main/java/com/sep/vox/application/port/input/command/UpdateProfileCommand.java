package com.sep.vox.application.port.input.command;

import java.time.LocalDate;

/**
 * Cập nhật hồ sơ của CHÍNH người gọi. Cố ý KHÔNG có {@code userId}: id luôn lấy từ
 * {@code UserContextPort}, nên không tồn tại đường sửa hồ sơ người khác qua mutation này. Sửa hồ sơ
 * người khác đi {@code updateSchoolUser} (school admin, đã chặn trong phạm vi trường của mình).
 *
 * <p>Cặp {@code x} + {@code xProvided} là quy ước PATCH của repo -- xem {@link UpdateSchoolUserCommand}
 * và mapper tương ứng. GraphQL không phân biệt được "không gửi trường" với "gửi null", nên mapper
 * đọc {@code input.containsKey(...)}. Nhờ vậy {@code avatarUrl: null} mang nghĩa XÓA ảnh, còn không
 * gửi {@code avatarUrl} thì giữ nguyên -- không phải đẻ thêm mutation removeAvatar chỉ để xóa.
 */
public record UpdateProfileCommand(
        String fullName,
        boolean fullNameProvided,
        String phone,
        boolean phoneProvided,
        String address,
        boolean addressProvided,
        LocalDate dateOfBirth,
        boolean dateOfBirthProvided,
        String avatarUrl,
        boolean avatarUrlProvided) {
}
