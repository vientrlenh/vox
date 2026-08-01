package com.sep.vox.application.response.input.schoolclassuser;

import java.util.UUID;

/** Một người dùng không thêm được vào lớp, kèm lý do hiển thị cho người dùng cuối. */
public record BulkCreateSchoolClassUserFailure(UUID userId, String reason) {

}
