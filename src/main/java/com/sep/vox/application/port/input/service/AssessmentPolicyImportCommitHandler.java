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

        // Cache theo input thô (đã trim) -> kết quả tra cứu, tránh query lặp lại khi nhiều dòng
        // trong file dùng chung 1 ngôn ngữ/khung/rubric/phạm vi/band (rất phổ biến trong thực tế).
        Map<String, Optional<UUID>> languageCache = new HashMap<>();
        Map<String, Optional<FrameworkVersion>> frameworkVersionCache = new HashMap<>();
        Map<String, Optional<RubricVersion>> rubricVersionCache = new HashMap<>();
        Map<UUID, Optional<Rubric>> rubricCache = new HashMap<>();
        Map<String, Optional<UUID>> scopeCache = new HashMap<>();
        Map<String, Optional<FrameworkResultBand>> bandCache = new HashMap<>();

        for (ImportRow row : rows) {
            if (row.getStatus() != ImportRowStatus.PENDING) continue;

            List<Map<String, String>> errors = new ArrayList<>();
            Map<String, String> rawData = jsonSerializationPort.toStringMap(row.getRawDataJson());
            Map<String, String> mappedData = applyMapping(mapping, rawData);

            try {
                validateRequiredFields(mappedData, errors);

                UUID languageId = errors.isEmpty()
                        ? resolveLanguage(mappedData.get("language"), errors, languageCache)
                        : null;

                FrameworkVersion frameworkVersion = errors.isEmpty()
                        ? resolveFrameworkVersion(mappedData.get("frameworkVersion"), errors, frameworkVersionCache)
                        : null;
                UUID frameworkVersionId = frameworkVersion != null ? frameworkVersion.getId() : null;

                UUID rubricVersionId = errors.isEmpty()
                        ? resolveRubricVersion(mappedData.get("rubricVersion"), frameworkVersion, schoolId, isSchoolScoped, errors, rubricVersionCache, rubricCache)
                        : null;

                ScopeIds scopeIds = (errors.isEmpty() && isSchoolScoped)
                        ? resolveScope(schoolId, mappedData.get("schoolClass"), mappedData.get("schoolGrade"), mappedData.get("schoolGradeLevel"), errors, scopeCache)
                        : ScopeIds.EMPTY;

                BandIds bandIds = errors.isEmpty()
                        ? resolveBands(frameworkVersionId, mappedData.get("targetFrameworkBand"), mappedData.get("minimumFrameworkBand"), errors, bandCache)
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
                        errors.add(error("scope", "Bị trùng phạm vi áp dụng ngay trong file Excel."));
                    } else if (assessmentPolicyRepository.existsActiveForScope(schoolId, languageId, frameworkVersionId,
                            scopeIds.schoolGradeLevelId(), scopeIds.schoolGradeId(), scopeIds.schoolClassId(), rubricVersionId)) {
                        errors.add(error("scope", "Đã tồn tại một Quy chế (DRAFT/PUBLISHED) khác cho cùng phạm vi này. Vui lòng lưu trữ (ARCHIVE) bản cũ."));
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
                errors.add(error("general", "Lỗi xử lý luồng ngầm: " + ex.getMessage()));
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

    private static void validateRequiredFields(Map<String, String> mappedData, List<Map<String, String>> errors) {
        requireField(mappedData.get("frameworkVersion"), "frameworkVersion", "Thiếu Phiên bản Khung năng lực.", errors);
        requireField(mappedData.get("rubricVersion"), "rubricVersion", "Thiếu Phiên bản Rubric.", errors);
        requireField(mappedData.get("language"), "language", "Thiếu Ngôn ngữ.", errors);
        requireField(mappedData.get("targetFrameworkBand"), "targetFrameworkBand", "Thiếu Band mục tiêu.", errors);
        requireField(mappedData.get("minimumFrameworkBand"), "minimumFrameworkBand", "Thiếu Band tối thiểu.", errors);
        requireField(mappedData.get("effectiveFrom"), "effectiveFrom", "Thiếu Ngày bắt đầu.", errors);
    }

    private static void requireField(String value, String field, String errorMessage, List<Map<String, String>> errors) {
        if (value == null || value.isBlank()) errors.add(error(field, errorMessage));
    }

    // Dual Lookup: cho phép người dùng điền mã HOẶC tên trong file Excel
    private UUID resolveLanguage(String languageInput, List<Map<String, String>> errors, Map<String, Optional<UUID>> cache) {
        String cleanInput = languageInput.trim();
        Optional<UUID> cached = cache.computeIfAbsent(cleanInput, key ->
                languageRepository.findByCode(key).or(() -> languageRepository.findByName(key)).map(l -> l.getId()));
        if (cached.isEmpty()) {
            errors.add(error("language", "Ngôn ngữ '" + cleanInput + "' không tồn tại trên hệ thống."));
            return null;
        }
        return cached.get();
    }

    private FrameworkVersion resolveFrameworkVersion(String fwVersionInput, List<Map<String, String>> errors, Map<String, Optional<FrameworkVersion>> cache) {
        String cleanInput = fwVersionInput.trim();
        Optional<FrameworkVersion> fwOpt = cache.computeIfAbsent(cleanInput, key ->
                frameworkVersionRepository.findByCode(key).or(() -> frameworkVersionRepository.findByName(key)));
        if (fwOpt.isEmpty()) {
            errors.add(error("frameworkVersion", "Không tìm thấy Phiên bản Khung: '" + cleanInput + "'"));
            return null;
        }
        FrameworkVersion frameworkVersion = fwOpt.get();
        if (frameworkVersion.getStatus() != FrameworkVersionStatus.PUBLISHED) {
            errors.add(error("frameworkVersion", "Phiên bản Khung này chưa được PUBLISHED."));
            return null;
        }
        return frameworkVersion;
    }

    private UUID resolveRubricVersion(String rubricVersionInput, FrameworkVersion frameworkVersion, UUID schoolId,
            boolean isSchoolScoped, List<Map<String, String>> errors,
            Map<String, Optional<RubricVersion>> rubricVersionCache, Map<UUID, Optional<Rubric>> rubricCache) {
        String cleanInput = rubricVersionInput.trim();
        Optional<RubricVersion> rubricOpt = rubricVersionCache.computeIfAbsent(cleanInput, key ->
                rubricVersionRepository.findByCode(key).or(() -> rubricVersionRepository.findByName(key)));
        if (rubricOpt.isEmpty()) {
            errors.add(error("rubricVersion", "Không tìm thấy Phiên bản Rubric: '" + cleanInput + "'"));
            return null;
        }

        RubricVersion rubricVersion = rubricOpt.get();
        if (rubricVersion.getStatus() == RubricStatus.PUBLISHED) {
            errors.add(error("rubricVersion", "Phiên bản Rubric này đã được PUBLISHED, không thể tạo Assessment Policy mới cho phiên bản này."));
            return null;
        }
        Rubric rubric = rubricCache.computeIfAbsent(rubricVersion.getRubricId(), rubricRepository::findById).orElse(null);
        if (rubric == null) {
            errors.add(error("rubricVersion", "Không tìm thấy Rubric gốc."));
        } else if (isSchoolScoped) {
            if (rubric.getOwnerType() != RubricOwnerType.SCHOOL || !schoolId.equals(rubric.getSchoolId())) {
                errors.add(error("rubricVersion", "Rubric Version không thuộc quyền quản lý của trường bạn."));
            } else if (!rubric.getFrameworkId().equals(frameworkVersion.getFrameworkId())) {
                errors.add(error("rubricVersion", "Rubric và Khung năng lực không khớp nhau."));
            }
        } else {
            if (rubric.getOwnerType() != RubricOwnerType.SYSTEM) {
                errors.add(error("rubricVersion", "Chỉ được dùng Rubric SYSTEM cho luồng System Admin."));
            } else if (!rubric.getFrameworkId().equals(frameworkVersion.getFrameworkId())) {
                errors.add(error("rubricVersion", "Rubric và Khung năng lực không khớp nhau."));
            }
        }
        return rubricVersion.getId();
    }

    // Phải điền đúng 1 trong 3: Lớp, Khối năm học (schoolGrade), hoặc Khối (schoolGradeLevel)
    private ScopeIds resolveScope(UUID schoolId, String classInput, String gradeInput, String gradeLevelInput,
            List<Map<String, String>> errors, Map<String, Optional<UUID>> cache) {
        boolean hasClass = classInput != null && !classInput.isBlank();
        boolean hasGrade = gradeInput != null && !gradeInput.isBlank();
        boolean hasGradeLevel = gradeLevelInput != null && !gradeLevelInput.isBlank();
        int scopeCount = (hasClass ? 1 : 0) + (hasGrade ? 1 : 0) + (hasGradeLevel ? 1 : 0);

        if (scopeCount != 1) {
            errors.add(error("scope", "Phải điền ĐÚNG 1 phạm vi áp dụng: Lớp, Khối năm học, HOẶC Khối."));
            return ScopeIds.EMPTY;
        }

        if (hasClass) {
            String cleanInput = classInput.trim();
            Optional<UUID> classId = cache.computeIfAbsent("class|" + cleanInput, key ->
                    schoolClassRepository.findBySchoolIdAndCode(schoolId, cleanInput)
                            .or(() -> schoolClassRepository.findBySchoolIdAndName(schoolId, cleanInput))
                            .map(c -> c.getId()));
            if (classId.isEmpty()) {
                errors.add(error("schoolClass", "Không tìm thấy Lớp '" + cleanInput + "'."));
                return ScopeIds.EMPTY;
            }
            return new ScopeIds(classId.get(), null, null);
        }

        if (hasGrade) {
            String cleanInput = gradeInput.trim();
            Optional<UUID> gradeId = cache.computeIfAbsent("grade|" + cleanInput, key ->
                    schoolGradeRepository.findBySchoolIdAndCode(schoolId, cleanInput)
                            .or(() -> schoolGradeRepository.findBySchoolIdAndName(schoolId, cleanInput))
                            .map(g -> g.getId()));
            if (gradeId.isEmpty()) {
                errors.add(error("schoolGrade", "Không tìm thấy Khối năm học '" + cleanInput + "'."));
                return ScopeIds.EMPTY;
            }
            return new ScopeIds(null, gradeId.get(), null);
        }

        String cleanInput = gradeLevelInput.trim();
        Optional<UUID> levelId = cache.computeIfAbsent("gradeLevel|" + cleanInput, key ->
                schoolGradeLevelRepository.findBySchoolIdAndCode(schoolId, cleanInput)
                        .or(() -> schoolGradeLevelRepository.findBySchoolIdAndName(schoolId, cleanInput))
                        .map(l -> l.getId()));
        if (levelId.isEmpty()) {
            errors.add(error("schoolGradeLevel", "Không tìm thấy Khối '" + cleanInput + "'."));
            return ScopeIds.EMPTY;
        }
        return new ScopeIds(null, null, levelId.get());
    }

    private BandIds resolveBands(UUID frameworkVersionId, String targetBandInput, String minimumBandInput,
            List<Map<String, String>> errors, Map<String, Optional<FrameworkResultBand>> cache) {
        String cleanTarget = targetBandInput.trim();
        Optional<FrameworkResultBand> targetBandOpt = cache.computeIfAbsent(frameworkVersionId + "|" + cleanTarget, key ->
                frameworkResultBandRepository.findByVersionIdAndCode(frameworkVersionId, cleanTarget)
                        .or(() -> frameworkResultBandRepository.findByVersionIdAndName(frameworkVersionId, cleanTarget)));

        String cleanMin = minimumBandInput.trim();
        Optional<FrameworkResultBand> minBandOpt = cache.computeIfAbsent(frameworkVersionId + "|" + cleanMin, key ->
                frameworkResultBandRepository.findByVersionIdAndCode(frameworkVersionId, cleanMin)
                        .or(() -> frameworkResultBandRepository.findByVersionIdAndName(frameworkVersionId, cleanMin)));

        if (targetBandOpt.isEmpty()) errors.add(error("targetFrameworkBand", "Band mục tiêu '" + cleanTarget + "' không tồn tại trong Khung này."));
        if (minBandOpt.isEmpty()) errors.add(error("minimumFrameworkBand", "Band tối thiểu '" + cleanMin + "' không tồn tại trong Khung này."));

        if (targetBandOpt.isPresent() && minBandOpt.isPresent()
                && minBandOpt.get().getOrder() > targetBandOpt.get().getOrder()) {
            errors.add(error("minimumFrameworkBand", "Band tối thiểu không được cao hơn Band mục tiêu."));
        }

        return new BandIds(
                targetBandOpt.map(FrameworkResultBand::getId).orElse(null),
                minBandOpt.map(FrameworkResultBand::getId).orElse(null));
    }

    private static BigDecimal parsePassingScore(String passingScoreStr, List<Map<String, String>> errors) {
        if (passingScoreStr == null || passingScoreStr.isBlank()) return null;
        try {
            BigDecimal passingScore = new BigDecimal(passingScoreStr.trim());
            if (passingScore.compareTo(BigDecimal.ZERO) < 0) errors.add(error("passingScore", "Điểm đạt không được âm."));
            return passingScore;
        } catch (NumberFormatException e) {
            errors.add(error("passingScore", "Điểm đạt phải là số hợp lệ."));
            return null;
        }
    }

    private static AssessmentPolicyStrictness parseStrictness(String strictnessStr, List<Map<String, String>> errors) {
        if (strictnessStr == null || strictnessStr.isBlank()) return AssessmentPolicyStrictness.STANDARD;
        try {
            return AssessmentPolicyStrictness.valueOf(strictnessStr.trim().toUpperCase());
        } catch (Exception e) {
            errors.add(error("strictness", "Mức độ nghiêm ngặt chỉ nhận LENIENT, STANDARD hoặc STRICT."));
            return AssessmentPolicyStrictness.STANDARD;
        }
    }

    private static EffectivePeriod parseEffectivePeriod(String effectiveFromStr, String effectiveToStr, List<Map<String, String>> errors) {
        try {
            OffsetDateTime effectiveFrom = DateMapper.toOffsetDateTime(effectiveFromStr.trim());
            OffsetDateTime effectiveTo = (effectiveToStr != null && !effectiveToStr.isBlank())
                    ? DateMapper.toOffsetDateTime(effectiveToStr.trim())
                    : null;
            if (effectiveTo != null && effectiveTo.isBefore(effectiveFrom)) {
                errors.add(error("effectiveTo", "Ngày kết thúc không được trước ngày bắt đầu."));
            }
            return new EffectivePeriod(effectiveFrom, effectiveTo);
        } catch (Exception e) {
            errors.add(error("effectiveFrom", "Định dạng ngày không hợp lệ."));
            return EffectivePeriod.EMPTY;
        }
    }

    private static Map<String, String> error(String field, String message) {
        return Map.of("field", field, "message", message);
    }
}
