package com.sep.vox.application.event;

import java.util.List;
import java.util.UUID;

/**
 * Một version của blueprint vừa được publish. Người nhận là mọi SCHOOL_ADMIN của trường.
 *
 * @param blueprintId cùng với {@code versionId} là thứ dựng được đường dẫn tới đúng version
 *        vừa publish; {@code blueprintCode} chỉ dùng để hiển thị, không có màn hình nào
 *        tra cứu theo mã
 */
public record ExamBlueprintVersionPublishedEvent(
    List<UUID> schoolAdminIds,
    String blueprintCode,
    String blueprintName,
    UUID blueprintId,
    UUID versionId
) {
}
