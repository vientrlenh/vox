package com.sep.vox.interfaces.rest.mapper;

import com.sep.vox.application.common.UploadedFile;
import com.sep.vox.application.port.input.command.PreviewRubricResultBandImportCommand;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

public class PreviewRubricResultBandImportCommandMapper {

    // Dành cho System Admin (Không có schoolId)
    public static PreviewRubricResultBandImportCommand fromSystemRequest(UUID versionId, MultipartFile file) {
        return new PreviewRubricResultBandImportCommand(null, versionId, toUploadedFile(file));
    }

    // Dành cho School Admin (Phải truyền kèm schoolId để bảo mật)
    public static PreviewRubricResultBandImportCommand fromSchoolRequest(UUID schoolId, UUID versionId, MultipartFile file) {
        return new PreviewRubricResultBandImportCommand(schoolId, versionId, toUploadedFile(file));
    }

    private static UploadedFile toUploadedFile(MultipartFile file) {
        try {
            return new UploadedFile(
                    file.getOriginalFilename(),
                    file.getContentType(),
                    file.getSize(),
                    file.getBytes()
            );
        } catch (IOException e) {
            throw new IllegalArgumentException("Hệ thống không thể xử lý dữ liệu từ file tải lên.");
        }
    }
}