package com.sep.vox.interfaces.rest.mapper;

import com.sep.vox.application.common.UploadedFile;
import com.sep.vox.application.port.input.command.PreviewFrameworkCriterionImportCommand;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

public class PreviewFrameworkCriterionImportCommandMapper {

    public static PreviewFrameworkCriterionImportCommand fromRequest(UUID versionId, MultipartFile file) {
        try {
            var uploadedFile = new UploadedFile(
                    file.getOriginalFilename(),
                    file.getContentType(),
                    file.getSize(),
                    file.getBytes()
            );
            return new PreviewFrameworkCriterionImportCommand(versionId, uploadedFile);
        } catch (IOException e) {
            throw new IllegalArgumentException("Hệ thống không thể đọc nội dung file tải lên. Vui lòng kiểm tra lại file.");
        }
    }
}
