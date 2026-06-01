package com.sep.vox.infrastructure.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.Properties;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.output.ImportFileData;

class LocalSchoolUserImportFileStorageServiceTests {

    private static final Path BASE_DIR = Path.of(System.getProperty("java.io.tmpdir"), "vox-imports");

    private LocalSchoolUserImportFileStorageService storageService;

    @BeforeEach
    void setUp() throws IOException {
        storageService = new LocalSchoolUserImportFileStorageService();
        cleanUpBaseDir();
    }

    @AfterEach
    void tearDown() throws IOException {
        cleanUpBaseDir();
    }

    @Test
    void load_should_reject_expired_file() throws IOException {
        var schoolId = UUID.randomUUID();
        var createdBy = UUID.randomUUID();
        var saved = storageService.save(
            new ImportFileData("students.csv", "text/csv", "email\nstudent@school.edu.vn\n".getBytes(StandardCharsets.UTF_8)),
            schoolId,
            createdBy
        );

        expireSavedFile(saved.fileId());

        assertThatThrownBy(() -> storageService.load(saved.fileId(), schoolId, createdBy))
            .isInstanceOf(NotFoundException.class)
            .hasMessage("File import đã hết hạn");

        assertThat(importFilePath(saved.fileId())).doesNotExist();
        assertThat(metaFilePath(saved.fileId())).doesNotExist();
    }

    @Test
    void load_should_return_active_file() throws IOException {
        var schoolId = UUID.randomUUID();
        var createdBy = UUID.randomUUID();
        var saved = storageService.save(
            new ImportFileData("students.csv", "text/csv", "email\nstudent@school.edu.vn\n".getBytes(StandardCharsets.UTF_8)),
            schoolId,
            createdBy
        );

        var resource = storageService.load(saved.fileId(), schoolId, createdBy);

        assertThat(resource.fileId()).isEqualTo(saved.fileId());
        assertThat(resource.originalFileName()).isEqualTo("students.csv");
        assertThat(resource.format()).isEqualTo("CSV");
        try (InputStream inputStream = resource.inputStream()) {
            assertThat(new String(inputStream.readAllBytes(), StandardCharsets.UTF_8))
                .contains("student@school.edu.vn");
        }
    }

    private void expireSavedFile(String fileId) throws IOException {
        var metaPath = metaFilePath(fileId);
        var props = new Properties();
        try (var input = Files.newInputStream(metaPath)) {
            props.load(input);
        }
        props.setProperty("expiresAt", OffsetDateTime.now().minusMinutes(1).toString());
        try (var output = Files.newOutputStream(metaPath)) {
            props.store(output, "vox import file metadata");
        }
    }

    private static void cleanUpBaseDir() throws IOException {
        if (!Files.exists(BASE_DIR)) {
            return;
        }
        try (var stream = Files.list(BASE_DIR)) {
            stream.forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                    
                }
            });
        }
    }

    private static Path importFilePath(String fileId) {
        return BASE_DIR.resolve("vox-import-" + fileId + ".csv");
    }

    private static Path metaFilePath(String fileId) {
        return BASE_DIR.resolve("vox-import-" + fileId + ".properties");
    }
}