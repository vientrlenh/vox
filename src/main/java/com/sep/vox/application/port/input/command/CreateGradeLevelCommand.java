package com.sep.vox.application.port.input.command;

// Không còn schoolId: khối lớp là catalog dùng chung toàn hệ thống, chỉ system admin tạo được.
public record CreateGradeLevelCommand(
        String code,
        String name,
        String description,
        Integer order
) {}
