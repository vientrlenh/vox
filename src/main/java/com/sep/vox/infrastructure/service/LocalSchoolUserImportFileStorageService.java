package com.sep.vox.infrastructure.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.Properties;
import java.util.UUID;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.sep.vox.application.common.importer.ImportFileFormat;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.output.ImportFileData;
import com.sep.vox.application.port.output.ImportFileResource;
import com.sep.vox.application.port.output.SchoolUserImportFileStoragePort;
import com.sep.vox.application.port.output.StoredImportFile;

@Service
public class LocalSchoolUserImportFileStorageService implements SchoolUserImportFileStoragePort {

    private static final String META_SUFFIX = ".properties";
    private static final String FILE_PREFIX = "vox-import-";
    private static final long FILE_TTL_HOURS = 1L;
    private static final String EXPIRES_AT_KEY = "expiresAt";
    private static final String ORIGINAL_NAME_KEY = "originalFileName";
    private static final String FORMAT_KEY = "format";
    private static final String SIZE_KEY = "sizeBytes";

    private final Path baseDir;

    public LocalSchoolUserImportFileStorageService() {
        this.baseDir = Path.of(System.getProperty("java.io.tmpdir"), "vox-imports");
        try {
            Files.createDirectories(baseDir);
        } catch (IOException e) {
            throw new IllegalStateException("Không thể khởi tạo thư mục lưu file import", e);
        }
    }

    @Override
    public StoredImportFile save(ImportFileData fileData) {
        var format = ImportFileFormat.fromFileName(fileData.originalFileName());
        var fileId = UUID.randomUUID().toString();
        var expiresAt = OffsetDateTime.now().plusHours(FILE_TTL_HOURS);
        var filePath = baseDir.resolve(FILE_PREFIX + fileId + "." + format.name().toLowerCase());
        var metaPath = baseDir.resolve(FILE_PREFIX + fileId + META_SUFFIX);

        try {
            Files.write(filePath, fileData.content());
            var props = new Properties();
            props.setProperty(ORIGINAL_NAME_KEY, fileData.originalFileName());
            props.setProperty(FORMAT_KEY, format.name());
            props.setProperty(SIZE_KEY, String.valueOf(fileData.content().length));
            props.setProperty(EXPIRES_AT_KEY, expiresAt.toString());
            try (var out = Files.newOutputStream(metaPath)) {
                props.store(out, "vox import file metadata");
            }
        } catch (IOException e) {
            throw new IllegalStateException("Không thể lưu file import", e);
        }

        return new StoredImportFile(fileId, fileData.originalFileName(), format.name(), fileData.content().length, expiresAt);
    }

    @Override
    public ImportFileResource load(String fileId) {
        var metaPath = baseDir.resolve(FILE_PREFIX + fileId + META_SUFFIX);
        if (!Files.exists(metaPath)) {
            throw new NotFoundException("Không tìm thấy file import");
        }
        try (var input = Files.newInputStream(metaPath)) {
            var props = new Properties();
            props.load(input);
            var format = props.getProperty(FORMAT_KEY);
            var originalName = props.getProperty(ORIGINAL_NAME_KEY);
            var sizeBytes = Long.parseLong(props.getProperty(SIZE_KEY, "0"));
            var expiresAt = OffsetDateTime.parse(props.getProperty(EXPIRES_AT_KEY));

            var filePath = baseDir.resolve(FILE_PREFIX + fileId + "." + format.toLowerCase());
            InputStream fileStream = Files.newInputStream(filePath);
            return new ImportFileResource(fileId, originalName, format, sizeBytes, expiresAt, fileStream);
        } catch (IOException e) {
            throw new IllegalStateException("Không thể đọc file import", e);
        }
    }

    @Override
    public void delete(String fileId) {
        var metaPath = baseDir.resolve(FILE_PREFIX + fileId + META_SUFFIX);
        try {
            if (!Files.exists(metaPath)) {
                return;
            }
            var props = new Properties();
            try (var input = Files.newInputStream(metaPath)) {
                props.load(input);
            }
            var format = props.getProperty(FORMAT_KEY);
            var filePath = baseDir.resolve(FILE_PREFIX + fileId + "." + format.toLowerCase());
            Files.deleteIfExists(filePath);
            Files.deleteIfExists(metaPath);
        } catch (IOException e) {
            throw new IllegalStateException("Không thể xóa file import", e);
        }
    }

    @Scheduled(cron = "0 0 2 * * *")
    public void cleanupExpiredFiles() {
        try (var stream = Files.list(baseDir)) {
            var now = OffsetDateTime.now();
            stream.filter(path -> path.getFileName().toString().startsWith(FILE_PREFIX) && path.getFileName().toString().endsWith(META_SUFFIX))
                .forEach(path -> cleanupMeta(path, now));
        } catch (IOException e) {
            throw new IllegalStateException("Không thể dọn dẹp file", e);
        }
    }

    private void cleanupMeta(Path metaPath, OffsetDateTime now) {
        var fileName = metaPath.getFileName().toString();
        var fileId = fileName.substring(FILE_PREFIX.length(), fileName.length() - META_SUFFIX.length());
        try (var input = Files.newInputStream(metaPath)) {
            var props = new Properties();
            props.load(input);
            var expiresAt = OffsetDateTime.parse(props.getProperty(EXPIRES_AT_KEY));
            if (expiresAt.isAfter(now)) {
                return;
            }
            var format = props.getProperty(FORMAT_KEY);
            var filePath = baseDir.resolve(FILE_PREFIX + fileId + "." + format.toLowerCase());
            Files.deleteIfExists(filePath);
            Files.deleteIfExists(metaPath);
        } catch (Exception e) {
            throw new IllegalStateException("Không thể dọn dẹp file", e);
        }
    }
}
