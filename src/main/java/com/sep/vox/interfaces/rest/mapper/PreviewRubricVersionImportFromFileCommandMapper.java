package com.sep.vox.interfaces.rest.mapper;

import com.sep.vox.application.common.UploadedFile;
import com.sep.vox.application.port.input.command.PreviewRubricVersionImportFromFileCommand;
import com.sep.vox.application.port.input.command.PreviewSchoolRubricVersionImportCommand;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

public class PreviewRubricVersionImportFromFileCommandMapper {

    public static PreviewRubricVersionImportFromFileCommand fromSystemRequest(UUID rubricId, MultipartFile file) {
        try {
            // Chuyển đổi từ MultipartFile của Spring sang UploadedFile của Domain
            var uploadedFile = new UploadedFile(
                    file.getOriginalFilename(),
                    file.getContentType(),
                    file.getSize(),
                    file.getBytes()
            );

            return new PreviewRubricVersionImportFromFileCommand(rubricId, uploadedFile);
        } catch (IOException e) {
            throw new IllegalArgumentException("Hệ thống không thể đọc nội dung file tải lên. Vui lòng kiểm tra lại file.");
        }
    }

    public static PreviewSchoolRubricVersionImportCommand fromSchoolRequest(UUID schoolId, UUID rubricId,  MultipartFile file) {
        try {
            // Chuyển đổi từ MultipartFile của Spring sang UploadedFile của Domain
            var uploadedFile = new UploadedFile(
                    file.getOriginalFilename(),
                    file.getContentType(),
                    file.getSize(),
                    file.getBytes()
            );

            return new PreviewSchoolRubricVersionImportCommand(schoolId, rubricId, uploadedFile);
        } catch (IOException e) {
            throw new IllegalArgumentException("Hệ thống không thể đọc nội dung file tải lên. Vui lòng kiểm tra lại file.");
        }
    }
}