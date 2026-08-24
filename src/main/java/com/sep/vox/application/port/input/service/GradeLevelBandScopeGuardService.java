package com.sep.vox.application.port.input.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * <p><b>Không áp dụng cho policy hệ thống:</b> CreateSystemAssessmentPolicyUseCase tạo policy với
 * cả bốn cột phạm vi là null (toàn hệ, chỉ theo ngôn ngữ + khung) -- không có khối nào để suy ra,
 * nên bảng trần theo khối về bản chất không phủ được luồng đó.
 *
 * <p><b>Trần thật sự phụ thuộc độ hẹp của phạm vi, không chỉ khối suy ra được:</b> bảng
 * {@code grade_level_band_scopes} chỉ seed 1 dòng defaultBand/hardMaxBand cho mỗi
 * (khối, framework) -- không phân biệt được "lớp chuyên" với lớp thường. Quy ước: chỉ policy neo
 * đúng vào 1 Lớp cụ thể mới được nới tới hardMaxBand đã seed; còn policy áp cho cả Khối hoặc cả
 * Niên khóa (gộp nhiều lớp, không tách được lớp nào chuyên) thì bị ép về defaultBand. Xem
 * {@link #effectiveCeilingBand}.
 */
@Service
public class GradeLevelBandScopeGuardService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GradeLevelBandScopeGuardService.class);

    private final GradeLevelBandScopeRepository bandScopeRepository;
    private final FrameworkResultBandRepository frameworkResultBandRepository;
    private final GradeLevelRepository gradeLevelRepository;
    private final SchoolGradeRepository schoolGradeRepository;
    private final SchoolClassRepository schoolClassRepository;

    public GradeLevelBandScopeGuardService(
            GradeLevelBandScopeRepository bandScopeRepository,
            FrameworkResultBandRepository frameworkResultBandRepository,
            GradeLevelRepository gradeLevelRepository,
            SchoolGradeRepository schoolGradeRepository,
            SchoolClassRepository schoolClassRepository) {
        this.bandScopeRepository = bandScopeRepository;
        this.frameworkResultBandRepository = frameworkResultBandRepository;
        this.gradeLevelRepository = gradeLevelRepository;
        this.schoolGradeRepository = schoolGradeRepository;
        this.schoolClassRepository = schoolClassRepository;
    }

    /**
     * Ném IllegalArgumentException nếu bậc mục tiêu vượt trần của khối tương ứng.
     *
     * <p>Truyền cả ba cột phạm vi đúng như policy đang dựng; chỉ một trong ba khác null.
     */
    public void assertWithinScope(UUID gradeLevelId, UUID schoolGradeId, UUID schoolClassId,
            UUID frameworkVersionId, FrameworkResultBand targetBand) {
        newBatch().assertWithinScope(gradeLevelId, schoolGradeId, schoolClassId, frameworkVersionId, targetBand);
    }

    /**
     * Bậc gợi ý mặc định của khối, để màn hình tạo policy điền sẵn. Rỗng khi khối chưa cấu hình
     * trần cho phiên bản khung này.
     */
    public Optional<FrameworkResultBand> defaultTargetBand(UUID gradeLevelId, UUID frameworkVersionId) {
        return bandScopeRepository.findByGradeLevelIdAndFrameworkVersionId(gradeLevelId, frameworkVersionId)
                .flatMap(scope -> frameworkResultBandRepository.findById(scope.getDefaultTargetBandId()));
    }

    /**
     * "hardMaxBand" ở đây là trần THẬT SỰ áp dụng cho lần gọi này, không phải giá trị seed thô.
     * Chỉ khi policy neo đúng vào 1 Lớp cụ thể (có thể là lớp chuyên) mới được nới tới hardMaxBand
     * đã seed cho khối (Bậc 4); còn policy áp cho cả Khối hoặc cả Niên khóa (nhiều lớp cùng lúc,
     * không phân biệt lớp nào là chuyên) thì chỉ được defaultBand -- xem {@link #effectiveCeilingBand}.
     */
    public record BandCeiling(FrameworkResultBand defaultBand, FrameworkResultBand hardMaxBand) {}

    /**
     * Bản đọc (không chặn gì) của {@link #assertWithinScope}, để màn hình tạo/sửa policy tự hiện
     * trần trước khi người dùng bấm submit thay vì chỉ biết bị chặn sau khi gọi API. Dùng lại đúng
     * logic suy khối (Lớp -> Khối năm học -> Khối) qua {@link Batch#resolveGradeLevelId}, nên FE và
     * BE không bao giờ lệch nhau về việc nào "mở"/"có trần". Rỗng nghĩa là mở, không có trần.
     */
    public Optional<BandCeiling> resolveCeiling(UUID gradeLevelId, UUID schoolGradeId, UUID schoolClassId,
            UUID frameworkVersionId) {
        UUID resolvedGradeLevelId = newBatch().resolveGradeLevelId(gradeLevelId, schoolGradeId, schoolClassId);
        if (resolvedGradeLevelId == null) {
            return Optional.empty();
        }
        return bandScopeRepository.findByGradeLevelIdAndFrameworkVersionId(resolvedGradeLevelId, frameworkVersionId)
                .flatMap(scope -> {
                    var defaultBand = frameworkResultBandRepository.findById(scope.getDefaultTargetBandId());
                    var seededHardMaxBand = frameworkResultBandRepository.findById(scope.getHardMaxBandId());
                    if (defaultBand.isEmpty() || seededHardMaxBand.isEmpty()) {
                        return Optional.<BandCeiling>empty();
                    }
                    FrameworkResultBand effectiveCeiling =
                            effectiveCeilingBand(schoolClassId, defaultBand.get(), seededHardMaxBand.get());
                    return Optional.of(new BandCeiling(defaultBand.get(), effectiveCeiling));
                });
    }

    /**
     * Chỉ policy neo đúng vào 1 Lớp cụ thể (schoolClassId khác null) mới được nới tới hardMaxBand đã
     * seed cho khối -- vì chỉ ở cấp Lớp mới phân biệt được "lớp chuyên" (cần bậc cao hơn) với lớp
     * thường. Policy áp cho cả Khối hoặc cả Niên khóa gộp nhiều lớp cùng lúc, không tách được lớp nào
     * là chuyên, nên chỉ được defaultBand.
     */
    private static FrameworkResultBand effectiveCeilingBand(UUID schoolClassId, FrameworkResultBand defaultBand,
            FrameworkResultBand seededHardMaxBand) {
        return schoolClassId != null ? seededHardMaxBand : defaultBand;
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
                // Không suy được khối -> không có trần nào để so. Chuỗi Lớp -> Khối năm học -> Khối
                // bị đứt là bất thường về dữ liệu (lớp trỏ tới năm học đã biến mất), nên kêu to.
                if (schoolClassId != null || schoolGradeId != null) {
                    LOGGER.warn(
                        "Không suy được Khối từ phạm vi (schoolClassId={}, schoolGradeId={}) -- bỏ qua"
                            + " kiểm trần bậc mục tiêu. Dữ liệu phân cấp lớp/năm học đang không nhất quán.",
                        schoolClassId, schoolGradeId
                    );
                }
                return;
            }

            var scope = findScope(resolvedGradeLevelId, frameworkVersionId);
            if (scope.isEmpty()) {
                // CHỦ ĐÍCH mở: cặp (khối, phiên bản khung) chưa khai trần thì không chặn gì cả.
                // Chặn hết sẽ làm đứng mọi luồng tạo policy ngay ngày đầu, vì V42 không seed dòng
                // trần nào. Đổi sang chặn mặc định chỉ cần sửa đúng nhánh này.
                LOGGER.debug(
                    "Khối {} chưa cấu hình trần bậc cho phiên bản khung {} -- không áp trần.",
                    resolvedGradeLevelId, frameworkVersionId
                );
                return;
            }

            var defaultBand = findBand(scope.get().getDefaultTargetBandId());
            var hardMaxBand = findBand(scope.get().getHardMaxBandId());
            if (defaultBand.isEmpty() || hardMaxBand.isEmpty()) {
                // FK trong V42 lẽ ra không cho phép xảy ra; nếu vẫn xảy ra thì cấu hình hỏng chứ
                // không phải người dùng sai -- không chặn họ, nhưng phải để lại dấu vết.
                LOGGER.error(
                    "Trần bậc của khối {} (phiên bản khung {}) trỏ tới bậc không tồn tại (default={}, hardMax={}).",
                    resolvedGradeLevelId, frameworkVersionId,
                    scope.get().getDefaultTargetBandId(), scope.get().getHardMaxBandId()
                );
                return;
            }

            FrameworkResultBand effectiveCeiling = effectiveCeilingBand(schoolClassId, defaultBand.get(), hardMaxBand.get());
            if (targetBand.getOrder() > effectiveCeiling.getOrder()) {
                throw new IllegalArgumentException(buildMessage(resolvedGradeLevelId, targetBand, effectiveCeiling));
            }
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
