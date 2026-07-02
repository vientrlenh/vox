package com.sep.vox.interfaces.rest.mapper;

import com.sep.vox.application.common.UploadedFile;
import com.sep.vox.application.port.input.command.PreviewRubricCriterionImportCommand;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.UUID;

public class PreviewRubricCriterionImportCommandMapper {

    // Dành cho System Admin
    public static PreviewRubricCriterionImportCommand fromSystemRequest(UUID versionId, MultipartFile file) {
        return new PreviewRubricCriterionImportCommand(null, versionId, toUploadedFile(file));
    }

    // Dành cho School Admin (Chuẩn bị sẵn)
    public static PreviewRubricCriterionImportCommand fromSchoolRequest(UUID schoolId, UUID versionId, MultipartFile file) {
        return new PreviewRubricCriterionImportCommand(schoolId, versionId, toUploadedFile(file));
    }

    private static UploadedFile toUploadedFile(MultipartFile file) {
        try {
            return new UploadedFile(file.getOriginalFilename(), file.getContentType(), file.getSize(), file.getBytes());
        } catch (IOException e) {
            throw new IllegalArgumentException("Hệ thống không thể xử lý dữ liệu từ file tải lên.");
        }
    }
}