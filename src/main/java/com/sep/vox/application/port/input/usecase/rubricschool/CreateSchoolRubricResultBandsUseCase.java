package com.sep.vox.application.port.input.usecase.rubricschool;

import com.sep.vox.domain.model.school.School;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.service.rubric.RubricOrderValidator;
import com.sep.vox.domain.service.rubric.RubricResultBandValidator;
import com.sep.vox.domain.service.rubric.ScoreRangeValidator;
import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.rubricschool.CreateSchoolRubricResultBandsCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.rubric.Rubric;
import com.sep.vox.domain.model.rubric.RubricResultBand;
import com.sep.vox.domain.model.rubric.RubricStatus;
import com.sep.vox.domain.model.rubric.RubricVersion;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.*;
import com.sep.vox.application.exception.DataConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
@RequiredArgsConstructor
public class CreateSchoolRubricResultBandsUseCase implements IUseCase<CreateSchoolRubricResultBandsCommand, List<UUID>> {

    private final RubricResultBandRepository rubricResultBandRepository;
    private final RubricVersionRepository rubricVersionRepository;
    private final RubricRepository rubricRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;
    private final SchoolRepository schoolRepository;
    private final SchoolUserRepository schoolUserRepository;

    @Override
    @Transactional
    public List<UUID> execute(CreateSchoolRubricResultBandsCommand command) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UnauthorizedException("Không tìm thấy tài khoản."));
        if (currentUser.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("Tài khoản của bạn đã bị khóa.");
        }

        RubricVersion version = loadDraftVersionOwnedBySchool(command, currentUserId);
        requireActiveSchool(command.schoolId());

        Set<String> uniqueCodes = new HashSet<>();
        // Bands already stored in the version plus the new bands of this batch, to check score-range overlap.
        // A TreeMap keyed by scoreMin checks each band in O(log n) instead of scanning a List in O(n)
        // (O(n log n) for the batch instead of O(n^2)).
        NavigableMap<BigDecimal, RubricResultBand> bandsSoFarByMin = new TreeMap<>();
        Set<Integer> ordersSoFar = collectStoredBands(command.versionId(), bandsSoFarByMin);

        List<RubricResultBand> bandsToSave = command.resultBands().stream()
                .map(bCmd -> toResultBand(version, bCmd, uniqueCodes, bandsSoFarByMin, ordersSoFar))
                .toList();

        // Take the ids from the saved bands (the database assigned them), not from the original bandsToSave.
        return saveBands(bandsToSave).stream().map(b -> b.getId()).toList();
    }

    // Fills bandsSoFarByMin with the stored bands and returns their orders, which the new orders of this
    // batch are accumulated into to reject duplicates.
    private Set<Integer> collectStoredBands(UUID versionId, NavigableMap<BigDecimal, RubricResultBand> bandsSoFarByMin) {
        Set<Integer> ordersSoFar = new HashSet<>();
        rubricResultBandRepository.findByRubricVersionId(versionId)
                .forEach(b -> {
                    bandsSoFarByMin.put(b.getScoreMin(), b);
                    ordersSoFar.add(b.getOrder());
                });
        return ordersSoFar;
    }

    private List<RubricResultBand> saveBands(List<RubricResultBand> bandsToSave) {
        try {
            return rubricResultBandRepository.saveAll(bandsToSave);
        } catch (DataConflictException e) {
            throw new IllegalArgumentException("Lỗi lưu dữ liệu: Mã thang điểm (Code) hoặc Thứ tự (order) đã tồn tại trong phiên bản Rubric này từ trước. Vui lòng kiểm tra lại.");
        }
    }

    private RubricVersion loadDraftVersionOwnedBySchool(CreateSchoolRubricResultBandsCommand command, UUID currentUserId) {
        RubricVersion version = rubricVersionRepository.findById(command.versionId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiên bản Rubric."));

        if (version.getStatus() != RubricStatus.DRAFT) {
            throw new IllegalArgumentException("Chỉ được thêm Thang điểm kết quả khi Rubric đang ở trạng thái DRAFT.");
        }

        Rubric rubric = rubricRepository.findById(version.getRubricId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bộ Rubric gốc."));

        SchoolUser schoolUser = schoolUserRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new ForbiddenException("Tài khoản của bạn không được liên kết với bất kỳ trường học nào."));

        if (rubric.getSchoolId() == null || !rubric.getSchoolId().equals(command.schoolId()) || !schoolUser.getSchoolId().equals(rubric.getSchoolId())) {
            throw new ForbiddenException("BẢO MẬT: Bạn không có quyền chỉnh sửa Rubric của trường khác.");
        }
        return version;
    }

    private void requireActiveSchool(UUID schoolId) {
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy trường học."));
        if (!school.isActive()) {
            throw new ForbiddenException("Hành động bị từ chối: Trường học này đang bị vô hiệu hóa trên hệ thống.");
        }
    }

    private void requireValidBand(RubricVersion version,
                                  CreateSchoolRubricResultBandsCommand.ResultBandItemCommand bCmd,
                                  String safeCode, String safeName, Set<String> uniqueCodes,
                                  NavigableMap<BigDecimal, RubricResultBand> bandsSoFarByMin,
                                  Set<Integer> ordersSoFar) {
        if (!uniqueCodes.add(safeCode)) {
            throw new IllegalArgumentException("Dữ liệu gửi lên bị trùng lặp Mã thang điểm (Code): " + safeCode);
        }

        if (bCmd.mappedScoreMin().compareTo(bCmd.mappedScoreMax()) > 0) {
            throw new IllegalArgumentException("Thang điểm '" + safeName + "': Điểm quy đổi tối thiểu không được lớn hơn tối đa.");
        }

        RubricResultBandValidator.assertNoOverlap(bandsSoFarByMin, bCmd.mappedScoreMin(), bCmd.mappedScoreMax(), safeName);
        RubricOrderValidator.assertNoDuplicateOrder(ordersSoFar, bCmd.order(), safeName);
        ScoreRangeValidator.assertWithinScale(version.getScoringScaleMin(), version.getScoringScaleMax(),
                bCmd.mappedScoreMin(), bCmd.mappedScoreMax(), safeName);
    }

    private RubricResultBand toResultBand(RubricVersion version,
                                          CreateSchoolRubricResultBandsCommand.ResultBandItemCommand bCmd,
                                          Set<String> uniqueCodes,
                                          NavigableMap<BigDecimal, RubricResultBand> bandsSoFarByMin,
                                          Set<Integer> ordersSoFar) {
        String safeCode = StringNormalization.trimAndCollapseSpaces(bCmd.code());
        String safeName = StringNormalization.trimAndCollapseSpaces(bCmd.name());

        requireValidBand(version, bCmd, safeCode, safeName, uniqueCodes, bandsSoFarByMin, ordersSoFar);

        // The id stays null so JPA treats this as a new record and the database generates the UUID.
        RubricResultBand band = RubricResultBand.create(
                version.getId(), safeCode, safeName,
                bCmd.description() != null ? StringNormalization.trimAndCollapseSpaces(bCmd.description()) : null,
                bCmd.mappedScoreMin(), bCmd.mappedScoreMax(), bCmd.order());
        bandsSoFarByMin.put(band.getScoreMin(), band);
        return band;
    }
}
