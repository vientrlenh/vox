package com.sep.vox.application.port.input.service;

import com.sep.vox.application.common.DateMapper;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicy;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicyStatus;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicyStrictness;
import com.sep.vox.domain.model.framework.FrameworkResultBand;
import com.sep.vox.domain.model.framework.FrameworkVersion;
import com.sep.vox.domain.model.framework.FrameworkVersionStatus;
import com.sep.vox.domain.model.importfile.ImportRow;
import com.sep.vox.domain.model.importfile.ImportRowStatus;
import com.sep.vox.domain.model.importfile.ImportSession;
import com.sep.vox.domain.model.importfile.ImportType;
import com.sep.vox.domain.model.rubric.Rubric;
import com.sep.vox.domain.model.rubric.RubricOwnerType;
import com.sep.vox.domain.model.rubric.RubricStatus;
import com.sep.vox.domain.model.rubric.RubricVersion;
import com.sep.vox.domain.repository.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;

@Component
public class AssessmentPolicyImportCommitHandler implements ImportCommitHandler {

    private final AssessmentPolicyRepository assessmentPolicyRepository;
    private final FrameworkVersionRepository frameworkVersionRepository;
    private final FrameworkResultBandRepository frameworkResultBandRepository;
    private final RubricVersionRepository rubricVersionRepository;
    private final RubricRepository rubricRepository;
    private final SupportedLanguageRepository languageRepository;
    private final SchoolGradeLevelRepository schoolGradeLevelRepository;
    private final SchoolGradeRepository schoolGradeRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final JsonSerializationPort jsonSerializationPort;

    public AssessmentPolicyImportCommitHandler(
            AssessmentPolicyRepository assessmentPolicyRepository,
            FrameworkVersionRepository frameworkVersionRepository,
            FrameworkResultBandRepository frameworkResultBandRepository,
            RubricVersionRepository rubricVersionRepository,
            RubricRepository rubricRepository,
            SupportedLanguageRepository languageRepository,
            SchoolGradeLevelRepository schoolGradeLevelRepository,
            SchoolGradeRepository schoolGradeRepository,
            SchoolClassRepository schoolClassRepository,
            JsonSerializationPort jsonSerializationPort) {
        this.assessmentPolicyRepository = assessmentPolicyRepository;
        this.frameworkVersionRepository = frameworkVersionRepository;
        this.frameworkResultBandRepository = frameworkResultBandRepository;
        this.rubricVersionRepository = rubricVersionRepository;
        this.rubricRepository = rubricRepository;
        this.languageRepository = languageRepository;
        this.schoolGradeLevelRepository = schoolGradeLevelRepository;
        this.schoolGradeRepository = schoolGradeRepository;
        this.schoolClassRepository = schoolClassRepository;
        this.jsonSerializationPort = jsonSerializationPort;
    }

    @Override
    public ImportType supportedType() {
        return ImportType.ASSESSMENT_POLICY;
    }

    // schoolGradeLevelId: Khối (tĩnh, không gắn năm học) | schoolGradeId: Khối năm học (khối trong 1 năm học cụ thể)
    // rubricVersionId nằm trong key vì trùng scope nhưng khác Rubric Version vẫn được phép (giống CreateSchoolAssessmentPolicyUseCase)
    private record ScopeKey(UUID schoolId, UUID languageId, UUID frameworkVersionId,
                            UUID schoolGradeLevelId, UUID schoolGradeId, UUID schoolClassId, UUID rubricVersionId) {}

    // Không gồm rubricVersionId: dùng để phát số "version" kế tiếp, vì DB chỉ unique theo scope+version
    // (không phân biệt Rubric Version), nên các Policy khác Rubric Version nhưng cùng scope này vẫn phải chiếm
    // các số version khác nhau.
    private record VersionScopeKey(UUID schoolId, UUID languageId, UUID frameworkVersionId,
                            UUID schoolGradeLevelId, UUID schoolGradeId, UUID schoolClassId) {}

    // Đúng 1 trong 3 phạm vi được điền: Lớp, Khối năm học, hoặc Khối
    private record ScopeIds(UUID schoolClassId, UUID schoolGradeId, UUID schoolGradeLevelId) {
        static final ScopeIds EMPTY = new ScopeIds(null, null, null);
    }

    private record BandIds(UUID targetBandId, UUID minimumBandId) {
        static final BandIds EMPTY = new BandIds(null, null);
    }

    private record EffectivePeriod(OffsetDateTime from, OffsetDateTime to) {
        static final EffectivePeriod EMPTY = new EffectivePeriod(null, null);
    }

    @Override
    public ImportCommitResult commit(ImportSession session, List<ImportRow> rows) {
        UUID schoolId = session.getSchoolId();
        boolean isSchoolScoped = schoolId != null;
        Map<String, String> mapping = resolveMapping(session);

        List<AssessmentPolicy> policiesToSave = new ArrayList<>();
        long importedCount = 0;
        long invalidCount = 0;
        OffsetDateTime now = OffsetDateTime.now();

        Set<ScopeKey> scopesClaimedInFile = new HashSet<>();
        Map<VersionScopeKey, Integer> nextVersionByScope = new HashMap<>();

        for (ImportRow row : rows) {
            if (row.getStatus() != ImportRowStatus.PENDING) continue;

            List<String> errors = new ArrayList<>();
            Map<String, String> rawData = jsonSerializationPort.toStringMap(row.getRawDataJson());
            Map<String, String> mappedData = applyMapping(mapping, rawData);

            try {
                validateRequiredFields(mappedData, errors);

                UUID languageId = errors.isEmpty()
                        ? resolveLanguage(mappedData.get("language"), errors)
                        : null;

                FrameworkVersion frameworkVersion = errors.isEmpty()
                        ? resolveFrameworkVersion(mappedData.get("frameworkVersion"), errors)
                        : null;
                UUID frameworkVersionId = frameworkVersion != null ? frameworkVersion.getId() : null;

                UUID rubricVersionId = errors.isEmpty()
                        ? resolveRubricVersion(mappedData.get("rubricVersion"), frameworkVersion, schoolId, isSchoolScoped, errors)
                        : null;

                ScopeIds scopeIds = (errors.isEmpty() && isSchoolScoped)
                        ? resolveScope(schoolId, mappedData.get("schoolClass"), mappedData.get("schoolGrade"), mappedData.get("schoolGradeLevel"), errors)
                        : ScopeIds.EMPTY;

                BandIds bandIds = errors.isEmpty()
                        ? resolveBands(frameworkVersionId, mappedData.get("targetFrameworkBand"), mappedData.get("minimumFrameworkBand"), errors)
                        : BandIds.EMPTY;

                BigDecimal passingScore = errors.isEmpty()
                        ? parsePassingScore(mappedData.get("passingScore"), errors)
                        : null;

                AssessmentPolicyStrictness strictness = errors.isEmpty()
                        ? parseStrictness(mappedData.get("strictness"), errors)
                        : AssessmentPolicyStrictness.STANDARD;

                EffectivePeriod effectivePeriod = errors.isEmpty()
                        ? parseEffectivePeriod(mappedData.get("effectiveFrom"), mappedData.get("effectiveTo"), errors)
                        : EffectivePeriod.EMPTY;

                ScopeKey scopeKey = null;
                if (errors.isEmpty()) {
                    scopeKey = new ScopeKey(schoolId, languageId, frameworkVersionId,
                            scopeIds.schoolGradeLevelId(), scopeIds.schoolGradeId(), scopeIds.schoolClassId(), rubricVersionId);
                    if (!scopesClaimedInFile.add(scopeKey)) {
                        errors.add("Bị trùng phạm vi áp dụng ngay trong file Excel.");
                    } else if (assessmentPolicyRepository.existsActiveForScope(schoolId, languageId, frameworkVersionId,
                            scopeIds.schoolGradeLevelId(), scopeIds.schoolGradeId(), scopeIds.schoolClassId(), rubricVersionId)) {
                        errors.add("Đã tồn tại một Quy chế (DRAFT/PUBLISHED) khác cho cùng phạm vi này. Vui lòng lưu trữ (ARCHIVE) bản cũ.");
                    }
                }

                if (!errors.isEmpty()) {
                    row.setStatus(ImportRowStatus.INVALID);
                    row.setErrorsJson(jsonSerializationPort.toJson(errors));
                    invalidCount++;
                    continue;
                }

                VersionScopeKey versionScopeKey = new VersionScopeKey(schoolId, languageId, frameworkVersionId,
                        scopeIds.schoolGradeLevelId(), scopeIds.schoolGradeId(), scopeIds.schoolClassId());
                int nextVersion = nextVersionByScope.computeIfAbsent(versionScopeKey, key ->
                        assessmentPolicyRepository.findMaxVersionForScope(
                                key.schoolId(), key.languageId(), key.frameworkVersionId(),
                                key.schoolGradeLevelId(), key.schoolGradeId(), key.schoolClassId()) + 1);
                nextVersionByScope.put(versionScopeKey, nextVersion + 1);

                AssessmentPolicy newPolicy = new AssessmentPolicy(
                        schoolId, scopeIds.schoolGradeLevelId(), scopeIds.schoolGradeId(), scopeIds.schoolClassId(),
                        languageId, frameworkVersionId, rubricVersionId, bandIds.targetBandId(), bandIds.minimumBandId(),
                        passingScore, strictness, nextVersion, AssessmentPolicyStatus.DRAFT,
                        effectivePeriod.from(), effectivePeriod.to(), now, now, session.getCreatedBy(), session.getCreatedBy()
                );
                policiesToSave.add(newPolicy);

                row.setStatus(ImportRowStatus.IMPORTED);
                row.setMappedDataJson(jsonSerializationPort.toJson(mappedData));
                importedCount++;
            } catch (Exception ex) {
                errors.add("Lỗi xử lý luồng ngầm: " + ex.getMessage());
                row.setStatus(ImportRowStatus.INVALID);
                row.setErrorsJson(jsonSerializationPort.toJson(errors));
                invalidCount++;
            }
        }

        if (!policiesToSave.isEmpty()) {
            assessmentPolicyRepository.saveAll(policiesToSave);
        }

        return new ImportCommitResult(importedCount, 0, 0, invalidCount);
    }

    private Map<String, String> resolveMapping(ImportSession session) {
        if (session.getConfirmedMappingJson() != null && !session.getConfirmedMappingJson().isBlank()) {
            return jsonSerializationPort.toStringMap(session.getConfirmedMappingJson());
        }
        if (session.getSuggestedMappingJson() != null && !session.getSuggestedMappingJson().isBlank()) {
            return jsonSerializationPort.toStringMap(session.getSuggestedMappingJson());
        }
        return new HashMap<>();
    }

    private static Map<String, String> applyMapping(Map<String, String> mapping, Map<String, String> rawData) {
        Map<String, String> mappedData = new HashMap<>();
        for (Map.Entry<String, String> entry : rawData.entrySet()) {
            String systemField = mapping.get(entry.getKey());
            if (systemField != null) {
                mappedData.put(systemField, entry.getValue());
            }
        }
        return mappedData;
    }

    private static void validateRequiredFields(Map<String, String> mappedData, List<String> errors) {
        requireField(mappedData.get("frameworkVersion"), "Thiếu Phiên bản Khung năng lực.", errors);
        requireField(mappedData.get("rubricVersion"), "Thiếu Phiên bản Rubric.", errors);
        requireField(mappedData.get("language"), "Thiếu Ngôn ngữ.", errors);
        requireField(mappedData.get("targetFrameworkBand"), "Thiếu Band mục tiêu.", errors);
        requireField(mappedData.get("minimumFrameworkBand"), "Thiếu Band tối thiểu.", errors);
        requireField(mappedData.get("effectiveFrom"), "Thiếu Ngày bắt đầu.", errors);
    }

    private static void requireField(String value, String errorMessage, List<String> errors) {
        if (value == null || value.isBlank()) errors.add(errorMessage);
    }

    // Dual Lookup: cho phép người dùng điền mã HOẶC tên trong file Excel
    private UUID resolveLanguage(String languageInput, List<String> errors) {
        String cleanInput = languageInput.trim();
        var language = languageRepository.findByCode(cleanInput).or(() -> languageRepository.findByName(cleanInput));
        if (language.isEmpty()) {
            errors.add("Ngôn ngữ '" + cleanInput + "' không tồn tại trên hệ thống.");
            return null;
        }
        return language.get().getId();
    }

    private FrameworkVersion resolveFrameworkVersion(String fwVersionInput, List<String> errors) {
        String cleanInput = fwVersionInput.trim();
        var fwOpt = frameworkVersionRepository.findByCode(cleanInput).or(() -> frameworkVersionRepository.findByName(cleanInput));
        if (fwOpt.isEmpty()) {
            errors.add("Không tìm thấy Phiên bản Khung: '" + cleanInput + "'");
            return null;
        }
        FrameworkVersion frameworkVersion = fwOpt.get();
        if (frameworkVersion.getStatus() != FrameworkVersionStatus.PUBLISHED) {
            errors.add("Phiên bản Khung này chưa được PUBLISHED.");
            return null;
        }
        return frameworkVersion;
    }

    private UUID resolveRubricVersion(String rubricVersionInput, FrameworkVersion frameworkVersion, UUID schoolId,
            boolean isSchoolScoped, List<String> errors) {
        String cleanInput = rubricVersionInput.trim();
        var rubricOpt = rubricVersionRepository.findByCode(cleanInput).or(() -> rubricVersionRepository.findByName(cleanInput));
        if (rubricOpt.isEmpty()) {
            errors.add("Không tìm thấy Phiên bản Rubric: '" + cleanInput + "'");
            return null;
        }

        RubricVersion rubricVersion = rubricOpt.get();
        if (rubricVersion.getStatus() == RubricStatus.PUBLISHED) {
            errors.add("Phiên bản Rubric này đã được PUBLISHED, không thể tạo Assessment Policy mới cho phiên bản này.");
            return null;
        }
        Rubric rubric = rubricRepository.findById(rubricVersion.getRubricId()).orElse(null);
        if (rubric == null) {
            errors.add("Không tìm thấy Rubric gốc.");
        } else if (isSchoolScoped) {
            if (rubric.getOwnerType() != RubricOwnerType.SCHOOL || !schoolId.equals(rubric.getSchoolId())) {
                errors.add("Rubric Version không thuộc quyền quản lý của trường bạn.");
            } else if (!rubric.getFrameworkId().equals(frameworkVersion.getFrameworkId())) {
                errors.add("Rubric và Khung năng lực không khớp nhau.");
            }
        }
        return rubricVersion.getId();
    }

    // Phải điền đúng 1 trong 3: Lớp, Khối năm học (schoolGrade), hoặc Khối (schoolGradeLevel)
    private ScopeIds resolveScope(UUID schoolId, String classInput, String gradeInput, String gradeLevelInput,
            List<String> errors) {
        boolean hasClass = classInput != null && !classInput.isBlank();
        boolean hasGrade = gradeInput != null && !gradeInput.isBlank();
        boolean hasGradeLevel = gradeLevelInput != null && !gradeLevelInput.isBlank();
        int scopeCount = (hasClass ? 1 : 0) + (hasGrade ? 1 : 0) + (hasGradeLevel ? 1 : 0);

        if (scopeCount != 1) {
            errors.add("Phải điền ĐÚNG 1 phạm vi áp dụng: Lớp, Khối năm học, HOẶC Khối.");
            return ScopeIds.EMPTY;
        }

        if (hasClass) {
            String cleanInput = classInput.trim();
            var classOpt = schoolClassRepository.findBySchoolIdAndCode(schoolId, cleanInput)
                    .or(() -> schoolClassRepository.findBySchoolIdAndName(schoolId, cleanInput));
            if (classOpt.isEmpty()) {
                errors.add("Không tìm thấy Lớp '" + cleanInput + "'.");
                return ScopeIds.EMPTY;
            }
            return new ScopeIds(classOpt.get().getId(), null, null);
        }

        if (hasGrade) {
            String cleanInput = gradeInput.trim();
            var gradeOpt = schoolGradeRepository.findBySchoolIdAndCode(schoolId, cleanInput)
                    .or(() -> schoolGradeRepository.findBySchoolIdAndName(schoolId, cleanInput));
            if (gradeOpt.isEmpty()) {
                errors.add("Không tìm thấy Khối năm học '" + cleanInput + "'.");
                return ScopeIds.EMPTY;
            }
            return new ScopeIds(null, gradeOpt.get().getId(), null);
        }

        String cleanInput = gradeLevelInput.trim();
        var levelOpt = schoolGradeLevelRepository.findBySchoolIdAndCode(schoolId, cleanInput)
                .or(() -> schoolGradeLevelRepository.findBySchoolIdAndName(schoolId, cleanInput));
        if (levelOpt.isEmpty()) {
            errors.add("Không tìm thấy Khối '" + cleanInput + "'.");
            return ScopeIds.EMPTY;
        }
        return new ScopeIds(null, null, levelOpt.get().getId());
    }

    private BandIds resolveBands(UUID frameworkVersionId, String targetBandInput, String minimumBandInput,
            List<String> errors) {
        String cleanTarget = targetBandInput.trim();
        var targetBandOpt = frameworkResultBandRepository.findByVersionIdAndCode(frameworkVersionId, cleanTarget)
                .or(() -> frameworkResultBandRepository.findByVersionIdAndName(frameworkVersionId, cleanTarget));

        String cleanMin = minimumBandInput.trim();
        var minBandOpt = frameworkResultBandRepository.findByVersionIdAndCode(frameworkVersionId, cleanMin)
                .or(() -> frameworkResultBandRepository.findByVersionIdAndName(frameworkVersionId, cleanMin));

        if (targetBandOpt.isEmpty()) errors.add("Band mục tiêu '" + cleanTarget + "' không tồn tại trong Khung này.");
        if (minBandOpt.isEmpty()) errors.add("Band tối thiểu '" + cleanMin + "' không tồn tại trong Khung này.");

        if (targetBandOpt.isPresent() && minBandOpt.isPresent()
                && minBandOpt.get().getOrder() > targetBandOpt.get().getOrder()) {
            errors.add("Band tối thiểu không được cao hơn Band mục tiêu.");
        }

        return new BandIds(
                targetBandOpt.map(FrameworkResultBand::getId).orElse(null),
                minBandOpt.map(FrameworkResultBand::getId).orElse(null));
    }

    private static BigDecimal parsePassingScore(String passingScoreStr, List<String> errors) {
        if (passingScoreStr == null || passingScoreStr.isBlank()) return null;
        try {
            BigDecimal passingScore = new BigDecimal(passingScoreStr.trim());
            if (passingScore.compareTo(BigDecimal.ZERO) < 0) errors.add("Điểm đạt không được âm.");
            return passingScore;
        } catch (NumberFormatException e) {
            errors.add("Điểm đạt phải là số hợp lệ.");
            return null;
        }
    }

    private static AssessmentPolicyStrictness parseStrictness(String strictnessStr, List<String> errors) {
        if (strictnessStr == null || strictnessStr.isBlank()) return AssessmentPolicyStrictness.STANDARD;
        try {
            return AssessmentPolicyStrictness.valueOf(strictnessStr.trim().toUpperCase());
        } catch (Exception e) {
            errors.add("Mức độ nghiêm ngặt chỉ nhận LENIENT, STANDARD hoặc STRICT.");
            return AssessmentPolicyStrictness.STANDARD;
        }
    }

    private static EffectivePeriod parseEffectivePeriod(String effectiveFromStr, String effectiveToStr, List<String> errors) {
        try {
            OffsetDateTime effectiveFrom = DateMapper.toOffsetDateTime(effectiveFromStr.trim());
            OffsetDateTime effectiveTo = (effectiveToStr != null && !effectiveToStr.isBlank())
                    ? DateMapper.toOffsetDateTime(effectiveToStr.trim())
                    : null;
            if (effectiveTo != null && effectiveTo.isBefore(effectiveFrom)) {
                errors.add("Ngày kết thúc không được trước ngày bắt đầu.");
            }
            return new EffectivePeriod(effectiveFrom, effectiveTo);
        } catch (Exception e) {
            errors.add("Định dạng ngày không hợp lệ.");
            return EffectivePeriod.EMPTY;
        }
    }
}
