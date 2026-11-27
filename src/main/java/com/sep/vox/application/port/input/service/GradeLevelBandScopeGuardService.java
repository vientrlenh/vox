package com.sep.vox.application.port.input.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.sep.vox.domain.model.framework.FrameworkResultBand;
import com.sep.vox.domain.model.gradelevel.GradeLevel;
import com.sep.vox.domain.model.gradelevel.GradeLevelBandScope;
import com.sep.vox.domain.repository.FrameworkResultBandRepository;
import com.sep.vox.domain.repository.GradeLevelBandScopeRepository;
import com.sep.vox.domain.repository.GradeLevelRepository;
import com.sep.vox.domain.repository.SchoolClassRepository;
import com.sep.vox.domain.repository.SchoolGradeRepository;

/**
 * Canh trần bậc mục tiêu theo khối lớp khi tạo/sửa Assessment Policy.
 *
 * <p><b>Vì sao cần đi vòng để tìm khối:</b> Assessment Policy bắt buộc chọn ĐÚNG MỘT phạm vi
 * trong ba (Lớp | Khối năm học | Khối) -- xem CreateSchoolAssessmentPolicyUseCase. Nghĩa là hai
 * phần ba số policy KHÔNG mang gradeLevelId, trong khi bảng trần lại khóa theo
 * (gradeLevelId, frameworkVersionId). Nếu chỉ canh khi policy chọn thẳng phạm vi Khối thì trần
 * bị vô hiệu chỉ bằng cách chọn phạm vi hẹp hơn -- mà chọn phạm vi Lớp lại là thao tác tự nhiên
 * hơn, nên trần sẽ trượt vì vô ý chứ không cần ai cố tình lách. Vì vậy ở đây luôn lần ngược
 * Lớp -> Khối năm học -> Khối trước khi so trần.
 *
 * <p><b>Hai loại bài kiểm tra, hai trần khác nhau:</b> cột {@code default_target_band_id} mang
 * nghĩa trần CENTRALIZE (kiểm tra tập trung -- policy áp cho cả Khối hoặc cả Niên khóa, tức
 * {@code schoolClassId} KHÔNG được chọn); cột {@code hard_max_band_id} mang nghĩa trần CLASS_TEST
 * (lớp chuyên có bài kiểm tra riêng -- policy neo đúng vào 1 {@code schoolClassId} cụ thể). Tên
 * cột giữ nguyên như lúc thiết kế ban đầu (không đổi schema), nhưng ngữ nghĩa THỰC TẾ là theo loại
 * bài kiểm tra, không phải "gợi ý mặc định" với "trần cứng". Chỉ đúng phạm vi Lớp mới được nới tới
 * trần CLASS_TEST; Khối và Niên khóa (gộp nhiều lớp, không tách được lớp nào chuyên) đều bị ép về
 * trần CENTRALIZE thấp hơn -- xem {@link Batch#effectiveCeilingBand}.
 *
 * <p><b>Không áp dụng cho policy hệ thống:</b> CreateSystemAssessmentPolicyUseCase tạo policy với
 * cả bốn cột phạm vi là null (toàn hệ, chỉ theo ngôn ngữ + khung) -- không có khối nào để suy ra,
 * nên bảng trần theo khối về bản chất không phủ được luồng đó.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GradeLevelBandScopeGuardService {

    private final GradeLevelBandScopeRepository bandScopeRepository;
    private final FrameworkResultBandRepository frameworkResultBandRepository;
    private final GradeLevelRepository gradeLevelRepository;
    private final SchoolGradeRepository schoolGradeRepository;
    private final SchoolClassRepository schoolClassRepository;

    /**
     * Throws IllegalArgumentException if the target band exceeds the ceiling of the corresponding grade level.
     *
     * <p>Pass all three scope columns exactly as the policy is being built; only one of the three is non-null.
     */
    public void assertWithinScope(UUID gradeLevelId, UUID schoolGradeId, UUID schoolClassId,
            UUID frameworkVersionId, FrameworkResultBand targetBand) {
        newBatch().assertWithinScope(gradeLevelId, schoolGradeId, schoolClassId, frameworkVersionId, targetBand);
    }

    /**
     * Trần CENTRALIZE (kiểm tra tập trung, không neo vào 1 Lớp cụ thể) của khối. Rỗng khi khối
     * chưa cấu hình trần cho phiên bản khung này.
     */
    public Optional<FrameworkResultBand> defaultTargetBand(UUID gradeLevelId, UUID frameworkVersionId) {
        return bandScopeRepository.findByGradeLevelIdAndFrameworkVersionId(gradeLevelId, frameworkVersionId)
                .flatMap(scope -> frameworkResultBandRepository.findById(scope.getDefaultTargetBandId()));
    }

    /**
     * Bản dùng cho import: cùng logic nhưng nhớ lại kết quả tra cứu trong một lần chạy.
     *
     * <p>Import Assessment Policy chạy hàng trăm dòng Excel dùng đi dùng lại vài khối và vài
     * khung -- gọi thẳng {@link #assertWithinScope} từng dòng sẽ thành N+1. Batch thu về
     * O(số giá trị khác nhau) truy vấn. KHÔNG dùng lại giữa các lần import (cache không có hạn).
     */
    public Batch newBatch() {
        return new Batch();
    }

    public final class Batch {

        private final Map<UUID, Optional<UUID>> gradeLevelIdBySchoolGradeId = new HashMap<>();
        private final Map<UUID, Optional<UUID>> schoolGradeIdBySchoolClassId = new HashMap<>();
        private final Map<ScopeKey, Optional<GradeLevelBandScope>> scopeByKey = new HashMap<>();
        private final Map<UUID, Optional<FrameworkResultBand>> bandById = new HashMap<>();
        private final Map<UUID, Optional<GradeLevel>> gradeLevelById = new HashMap<>();

        private Batch() {}

        public void assertWithinScope(UUID gradeLevelId, UUID schoolGradeId, UUID schoolClassId,
                UUID frameworkVersionId, FrameworkResultBand targetBand) {
            if (targetBand == null || frameworkVersionId == null) {
                return;
            }

            UUID resolvedGradeLevelId = resolveGradeLevelId(gradeLevelId, schoolGradeId, schoolClassId);
            if (resolvedGradeLevelId == null) {
                warnGradeLevelNotResolved(schoolClassId, schoolGradeId);
                return;
            }

            Optional<FrameworkResultBand> effectiveCeiling =
                    findEffectiveCeiling(resolvedGradeLevelId, frameworkVersionId, schoolClassId);
            if (effectiveCeiling.isPresent() && targetBand.getOrder() > effectiveCeiling.get().getOrder()) {
                throw new IllegalArgumentException(
                        buildMessage(resolvedGradeLevelId, targetBand, effectiveCeiling.get()));
            }
        }

        private void warnGradeLevelNotResolved(UUID schoolClassId, UUID schoolGradeId) {
            // The grade level cannot be derived -> there is no ceiling to compare against. A broken Class -> School grade -> Grade level
            // chain is abnormal data (a class pointing to a school year that has disappeared), so shout.
            if (schoolClassId != null || schoolGradeId != null) {
                log.warn(
                    "Không suy được Khối từ phạm vi (schoolClassId={}, schoolGradeId={}) -- bỏ qua"
                        + " kiểm trần bậc mục tiêu. Dữ liệu phân cấp lớp/năm học đang không nhất quán.",
                    schoolClassId, schoolGradeId
                );
            }
        }

        private Optional<FrameworkResultBand> findEffectiveCeiling(
                UUID resolvedGradeLevelId, UUID frameworkVersionId, UUID schoolClassId) {
            Optional<GradeLevelBandScope> scope = findScope(resolvedGradeLevelId, frameworkVersionId);
            if (scope.isEmpty()) {
                // DELIBERATELY open: a (grade level, framework version) pair with no declared ceiling blocks nothing.
                // Blocking everything would stall every policy-creation flow on day one, because V42 seeds no
                // ceiling row. Switching to block-by-default only needs a change in exactly this branch.
                log.debug(
                    "Khối {} chưa cấu hình trần bậc cho phiên bản khung {} -- không áp trần.",
                    resolvedGradeLevelId, frameworkVersionId
                );
                return Optional.empty();
            }
            return ceilingOfScope(scope.get(), resolvedGradeLevelId, frameworkVersionId, schoolClassId);
        }

        private Optional<FrameworkResultBand> ceilingOfScope(GradeLevelBandScope scope,
                UUID resolvedGradeLevelId, UUID frameworkVersionId, UUID schoolClassId) {
            Optional<FrameworkResultBand> centralizeCapBand = findBand(scope.getDefaultTargetBandId());
            Optional<FrameworkResultBand> classTestCapBand = findBand(scope.getHardMaxBandId());
            if (centralizeCapBand.isEmpty() || classTestCapBand.isEmpty()) {
                // The FK in V42 should make this impossible; if it still happens the configuration is broken and
                // the user is not at fault -- do not block them, but leave a trace.
                log.error(
                    "Trần bậc của khối {} (phiên bản khung {}) trỏ tới bậc không tồn tại"
                        + " (centralize={}, classTest={}).",
                    resolvedGradeLevelId, frameworkVersionId,
                    scope.getDefaultTargetBandId(), scope.getHardMaxBandId()
                );
                return Optional.empty();
            }
            return Optional.of(effectiveCeilingBand(schoolClassId, centralizeCapBand.get(), classTestCapBand.get()));
        }

        /**
         * Chỉ policy neo đúng vào 1 Lớp cụ thể ({@code schoolClassId} khác null) mới là bài
         * CLASS_TEST, được nới tới trần {@code classTestCapBand}. Policy áp cho cả Khối hoặc cả
         * Niên khóa (gộp nhiều lớp cùng lúc, không tách được lớp nào chuyên) là bài CENTRALIZE,
         * chỉ được trần {@code centralizeCapBand} thấp hơn.
         */
        private static FrameworkResultBand effectiveCeilingBand(UUID schoolClassId,
                FrameworkResultBand centralizeCapBand, FrameworkResultBand classTestCapBand) {
            return schoolClassId != null ? classTestCapBand : centralizeCapBand;
        }

        /**
         * Lần ngược từ phạm vi hẹp về Khối: Lớp -> Khối năm học -> Khối.
         *
         * @return id Khối, hoặc null khi phạm vi không dẫn tới khối nào.
         */
        public UUID resolveGradeLevelId(UUID gradeLevelId, UUID schoolGradeId, UUID schoolClassId) {
            if (gradeLevelId != null) {
                return gradeLevelId;
            }
            UUID effectiveGradeId = schoolGradeId;
            if (effectiveGradeId == null && schoolClassId != null) {
                effectiveGradeId = schoolGradeIdBySchoolClassId
                        .computeIfAbsent(schoolClassId, id -> schoolClassRepository.findById(id)
                                .map(schoolClass -> schoolClass.getSchoolGradeId()))
                        .orElse(null);
            }
            if (effectiveGradeId == null) {
                return null;
            }
            return gradeLevelIdBySchoolGradeId
                    .computeIfAbsent(effectiveGradeId, id -> schoolGradeRepository.findById(id)
                            .map(schoolGrade -> schoolGrade.getGradeLevelId()))
                    .orElse(null);
        }

        private Optional<GradeLevelBandScope> findScope(UUID gradeLevelId, UUID frameworkVersionId) {
            return scopeByKey.computeIfAbsent(
                    new ScopeKey(gradeLevelId, frameworkVersionId),
                    key -> bandScopeRepository.findByGradeLevelIdAndFrameworkVersionId(
                            key.gradeLevelId(), key.frameworkVersionId()));
        }

        private Optional<FrameworkResultBand> findBand(UUID bandId) {
            return bandById.computeIfAbsent(bandId, frameworkResultBandRepository::findById);
        }

        private String buildMessage(UUID gradeLevelId, FrameworkResultBand targetBand, FrameworkResultBand hardMaxBand) {
            String gradeLevelName = gradeLevelById
                    .computeIfAbsent(gradeLevelId, gradeLevelRepository::findById)
                    .map(level -> level.getName())
                    .orElse("khối đã chọn");
            return "Bậc mục tiêu '" + targetBand.getLabel() + "' vượt quá trần của " + gradeLevelName
                    + ". Bậc cao nhất được phép là '" + hardMaxBand.getLabel() + "'.";
        }
    }

    private record ScopeKey(UUID gradeLevelId, UUID frameworkVersionId) {}
}
