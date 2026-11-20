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
     * Ca còn hiệu lực: sẽ diễn ra, hoặc đã diễn ra thật. Ca đã huỷ/dời/xoá thì không -- mọi luật
     * nghiệp vụ nói "ca thi nào cũng phải..." đều ngầm hiểu là ca còn hiệu lực, vì bắt một ca không
     * bao giờ diễn ra phải đủ phòng/giám thị thì chỉ chặn được người dùng chứ không bảo vệ buổi thi.
     */
    public boolean isInEffect() {
        return !isRemoved() && this != CANCELLED;
    }

    /**
     * Chỉ ca còn hiệu lực mới điểm danh được -- ca đã huỷ/dời/xoá thì không.
     */
    public boolean allowsAttendance() {
        return isInEffect();
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
