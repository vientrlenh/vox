package com.sep.vox.interfaces.graphql.mapper;

import java.time.LocalDate;
import java.util.Map;

import com.sep.vox.application.port.input.command.UpdateProfileCommand;

/**
 * Nhận {@code Map} thô chứ không phải record: đó là cách DUY NHẤT phân biệt "client không gửi
 * trường" với "client gửi null" trong GraphQL -- record đã bind xong thì hai trường hợp đó đều ra
 * null. Cặp {@code containsKey} + giá trị chính là thứ làm nên ngữ nghĩa PATCH, và nhờ nó
 * {@code avatarUrl: null} mới mang nghĩa GỠ ảnh thay vì bị bỏ qua.
 *
 * <p>Cùng khuôn với {@link UpdateSchoolUserCommandMapper} -- sửa một bên thì ngó sang bên kia.
 */
public final class UpdateProfileCommandMapper {

    private UpdateProfileCommandMapper() {
    }

    public static UpdateProfileCommand fromInput(Map<String, Object> input) {
        return new UpdateProfileCommand(
            valueOf(input.get("fullName")),
            input.containsKey("fullName"),
            valueOf(input.get("phone")),
            input.containsKey("phone"),
            valueOf(input.get("address")),
            input.containsKey("address"),
            parseDateOfBirth(input.get("dateOfBirth")),
            input.containsKey("dateOfBirth"),
            valueOf(input.get("avatarUrl")),
            input.containsKey("avatarUrl")
        );
    }

    private static String valueOf(Object value) {
        return value == null ? null : value.toString();
    }

    private static LocalDate parseDateOfBirth(Object value) {
        if (value == null) return null;
        return LocalDate.parse(value.toString());
    }
}
