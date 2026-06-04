package com.sep.vox.infrastructure.service;

import java.util.Locale;

import org.springframework.stereotype.Service;

import com.sep.vox.application.port.output.FileProcessingPort;

@Service
public class FileProcessingService implements FileProcessingPort {
    
    private static final String[] ALLOWED_FILES = {
        "Excel", 
        "Csv"
    };

    @Override
    public void importFile() {
        
    }

    private String extensionOf(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return null;
        }
        var dotIndex = fileName.lastIndexOf(".");
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            return null;
        }
        return fileName.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }
}
