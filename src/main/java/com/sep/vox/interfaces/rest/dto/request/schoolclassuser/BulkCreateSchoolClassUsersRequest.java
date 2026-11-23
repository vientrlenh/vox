package com.sep.vox.interfaces.rest.dto.request.schoolclassuser;

import com.sep.vox.application.port.input.command.schoolclassuser.BulkCreateSchoolClassUsersCommand;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record BulkCreateSchoolClassUsersRequest(
    @NotEmpty(message = "Danh sách người dùng không được để trống")
    @Size(max = 200, message = "Chỉ có thể thêm tối đa 200 người dùng mỗi lần")
    List<UUID> userIds
) {

    public static BulkCreateSchoolClassUsersCommand toCommand(UUID schoolId, UUID classId,
            BulkCreateSchoolClassUsersRequest request) {
        return new BulkCreateSchoolClassUsersCommand(schoolId, classId, request.userIds());
    }
}
