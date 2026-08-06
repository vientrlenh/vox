package com.sep.vox.application.query.repository;

import static com.sep.vox.application.response.input.practiceinsights.PracticeInsights.WeaknessProfile;

import java.util.UUID;

/**
 * Chỉ còn hồ sơ điểm yếu mức TIÊU CHÍ.
 *
 * <p>Trước đây còn 4 phép đọc nữa: đường tiến bộ theo tiêu chí ({@code progress}), bảng tổng
 * quan lớp ({@code classOverview}) và hai chốt quyền giáo viên đi kèm. Cả bốn phục vụ những
 * màn hình chưa từng được viết -- không client nào (Flutter lẫn web) gọi tới, kiểm bằng cách
 * đối chiếu toàn bộ field GraphQL với mã nguồn hai client.
 */
public interface PracticeInsightsQueryRepository {

    WeaknessProfile weaknessProfile(UUID studentId);
}
