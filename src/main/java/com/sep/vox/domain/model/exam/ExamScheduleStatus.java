package com.sep.vox.domain.model.exam;

public enum ExamScheduleStatus {
    DRAFT,
    PUBLISHED,
    COMPLETED,
    MOVED,
    CANCELLED,
    DELETED;

    /**
     * Ca đã xoá mềm hoặc đã dời hết người sang ca khác thì coi như không còn tồn tại: mọi màn đọc
     * (lịch của học sinh, lịch giám thị, điểm danh) phải bỏ qua. CANCELLED vẫn hiện để người dùng
     * biết ca bị huỷ.
     */
    public boolean isRemoved() {
        return this == MOVED || this == DELETED;
    }

    /**
     * Chỉ ca còn hiệu lực mới điểm danh được -- ca đã huỷ/dời/xoá thì không.
     */
    public boolean allowsAttendance() {
        return !isRemoved() && this != CANCELLED;
    }

    /**
     * Ca thi học sinh được phép nhìn thấy. DRAFT là ca chưa publish -- tức lịch thi chưa xếp xong --
     * nên không được lộ cho thí sinh; MOVED/DELETED thì đã bị thay thế/xoá. CANCELLED vẫn hiện để
     * học sinh biết ca đã bị huỷ.
     */
    public boolean isVisibleToStudent() {
        return this == PUBLISHED || this == COMPLETED || this == CANCELLED;
    }
}
