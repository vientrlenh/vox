package com.sep.vox.interfaces.rest.mapper;

import com.sep.vox.application.common.UploadedFile;
import com.sep.vox.application.port.input.command.PreviewRubricCriterionBandImportCommand;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.UUID;

public class PreviewRubricCriterionBandImportCommandMapper {


    public static PreviewRubricCriterionBandImportCommand fromSystemRequest(UUID criterionId, MultipartFile file) {
        return new PreviewRubricCriterionBandImportCommand(null, criterionId, toUploadedFile(file));
    }

    public static PreviewRubricCriterionBandImportCommand fromSchoolRequest(UUID schoolId, UUID criterionId, MultipartFile file) {
        return new PreviewRubricCriterionBandImportCommand(schoolId, criterionId, toUploadedFile(file));
    }

    private static UploadedFile toUploadedFile(MultipartFile file) {
        try {
            return new UploadedFile(file.getOriginalFilename(), file.getContentType(), file.getSize(), file.getBytes());
        } catch (IOException e) {
            throw new IllegalArgumentException("Hệ thống không thể xử lý dữ liệu từ file tải lên.");
        }
    }
}