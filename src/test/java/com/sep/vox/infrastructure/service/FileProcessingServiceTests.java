package com.sep.vox.infrastructure.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.sep.vox.application.common.UploadedFile;
import com.sep.vox.application.response.output.ParseImportFileResult;
import com.sep.vox.domain.model.importfile.ImportType;

class FileProcessingServiceTests {

    @Test
    void parse_should_suggest_mapping_for_school_class_user_import() {
        FileProcessingService service = new FileProcessingService();
        byte[] content = "Email,Mã lớp\nstudent@example.com,ENG-01\n".getBytes(StandardCharsets.UTF_8);
        UploadedFile file = UploadedFile.upload("class-users.csv", "text/csv", content.length, content);

        ParseImportFileResult result = service.parse(file, ImportType.SCHOOL_CLASS_USER);

        assertThat(result.originalHeaders()).containsExactly("Email", "Mã lớp");
        assertThat(result.suggestedMapping()).containsEntry("Email", "email");
        assertThat(result.suggestedMapping()).containsEntry("Mã lớp", "classCode");
        assertThat(result.totalRows()).isEqualTo(1L);
    }
}
