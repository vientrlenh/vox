package com.sep.vox.interfaces.rest.mapper;

import com.sep.vox.application.common.UploadedFile;
import com.sep.vox.application.port.input.command.rubricsystem.PreviewRubricCriterionImportCommand;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.UUID;

public class PreviewRubricCriterionImportCommandMapper {

    public static PreviewRubricCriterionImportCommand fromSystemRequest(UUID versionId, MultipartFile file) {
        return new PreviewRubricCriterionImportCommand(null, versionId, toUploadedFile(file));
    }

    public static PreviewRubricCriterionImportCommand fromSchoolRequest(UUID schoolId, UUID versionId, MultipartFile file) {
        return new PreviewRubricCriterionImportCommand(schoolId, versionId, toUploadedFile(file));
    }

    private static UploadedFile toUploadedFile(MultipartFile file) {
        try {
            return new UploadedFile(file.getOriginalFilename(), file.getContentType(), file.getSize(), file.getBytes());
        } catch (IOException e) {
            throw new IllegalArgumentException("The system cannot process the data from the uploaded file.");
        }
    }
}