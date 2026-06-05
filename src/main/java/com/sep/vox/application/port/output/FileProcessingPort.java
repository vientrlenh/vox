package com.sep.vox.application.port.output;

import com.sep.vox.application.common.UploadedFile;
import com.sep.vox.application.response.output.ParseImportFileResult;
import com.sep.vox.domain.model.importfile.ImportType;

public interface FileProcessingPort {
    ParseImportFileResult parse(UploadedFile file, ImportType type);
    
}
