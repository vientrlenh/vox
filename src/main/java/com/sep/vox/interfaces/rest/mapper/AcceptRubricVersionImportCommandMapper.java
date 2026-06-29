package com.sep.vox.interfaces.rest.mapper;

import com.sep.vox.application.port.input.command.AcceptSchoolRubricVersionImportCommand;
import com.sep.vox.application.port.input.command.AcceptSystemRubricVersionImportCommand;
import com.sep.vox.interfaces.rest.dto.request.AcceptImportRequest;

import java.util.UUID;

public class AcceptRubricVersionImportCommandMapper {
    // 👉 Map từ Request DTO chuẩn chỉ vào Command cho System Admin
    public static AcceptSystemRubricVersionImportCommand fromSystemRequest(UUID sessionId, AcceptImportRequest request) {
        return new AcceptSystemRubricVersionImportCommand(sessionId, request.confirmedMapping());
    }

    // Tiện tay nâng cấp luôn hàm cho School Admin nếu bác cần sau này
    public static AcceptSchoolRubricVersionImportCommand fromSchoolRequestId(UUID schoolId, UUID sessionId, AcceptImportRequest request) {
        return new AcceptSchoolRubricVersionImportCommand(schoolId, sessionId, request.confirmedMapping());
    }

}