package com.sep.vox.interfaces.rest.mapper;

import com.sep.vox.application.common.UploadedFile;
import com.sep.vox.application.port.input.command.PreviewSchoolAssessmentPolicyImportFromFileCommand;
import com.sep.vox.application.port.input.command.PreviewSystemAssessmentPolicyImportFromFileCommand;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

public class PreviewAssessmentPolicyImportFromFileCommandMapper {

    public static PreviewSystemAssessmentPolicyImportFromFileCommand fromSystemRequest(MultipartFile file) {
        try {
            var uploadedFile = new UploadedFile(
                    file.getOriginalFilename(),
                    file.getContentType(),
                    file.getSize(),
                    file.getBytes()
            );
            return new PreviewSystemAssessmentPolicyImportFromFileCommand(uploadedFile);
        } catch (IOException e) {
            throw new IllegalArgumentException("Hệ thống không thể đọc nội dung file tải lên. Vui lòng kiểm tra lại file.");
        }
    }

    public static PreviewSchoolAssessmentPolicyImportFromFileCommand fromSchoolRequest(UUID schoolId, MultipartFile file) {
        try {
            var uploadedFile = new UploadedFile(
                    file.getOriginalFilename(),
                    file.getContentType(),
                    file.getSize(),
                    file.getBytes()
            );
            return new PreviewSchoolAssessmentPolicyImportFromFileCommand(schoolId, uploadedFile);
        } catch (IOException e) {
            throw new IllegalArgumentException("Hệ thống không thể đọc nội dung file tải lên. Vui lòng kiểm tra lại file.");
        }
    }
}
