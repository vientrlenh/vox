package com.sep.vox.domain.common;

/** Cách nhà trường chia hạn mức cá nhân cho giáo viên/học sinh -- xem DistributeQuotaToUsersService. */
public enum DistributionMode {
    /** Chia đều ví của trường cho mọi người đủ điều kiện, phần dư rải cho những người đầu danh sách. */
    AUTO,
    /** Nhà trường tự nhập số cho từng người. */
    MANUAL
}
