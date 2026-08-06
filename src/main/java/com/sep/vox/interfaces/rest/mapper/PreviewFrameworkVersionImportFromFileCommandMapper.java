package com.sep.vox.interfaces.rest.mapper;

import com.sep.vox.application.common.UploadedFile;
import com.sep.vox.application.port.input.command.PreviewFrameworkVersionImportCommand;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

public class PreviewFrameworkVersionImportFromFileCommandMapper {

    public static PreviewFrameworkVersionImportCommand fromRequest(UUID frameworkId, MultipartFile file) {
        try {
            var uploadedFile = new UploadedFile(
                    file.getOriginalFilename(),
                    file.getContentType(),
                    file.getSize(),
                    file.getBytes()
            );
            return new PreviewFrameworkVersionImportCommand(frameworkId, uploadedFile);
        } catch (IOException e) {
            throw new IllegalArgumentException("Hệ thống không thể đọc nội dung file tải lên. Vui lòng kiểm tra lại file.");
        }
    }
}
