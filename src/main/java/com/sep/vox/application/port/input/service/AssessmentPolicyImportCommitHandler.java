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
import com.sep.vox.domain.model.school.SchoolClass;
import com.sep.vox.domain.model.school.SchoolGrade;
import com.sep.vox.domain.model.school.SchoolGradeLevel;
import com.sep.vox.domain.model.supportedlanguage.SupportedLanguage;
import com.sep.vox.domain.repository.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
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

    private record BandIds(UUID targetBandId) {
        static final BandIds EMPTY = new BandIds(null);
    }

    private record EffectivePeriod(OffsetDateTime from, OffsetDateTime to) {
        static final EffectivePeriod EMPTY = new EffectivePeriod(null, null);
    }

    // Toàn bộ dữ liệu tham chiếu (ngôn ngữ/khung/rubric/scope/band) đã prefetch 1 lần bằng số query CỐ ĐỊNH
    // (không phụ thuộc số dòng Excel lẫn số giá trị distinct), dùng để resolve thuần trong memory ở Phase 2.
    private record LookupContext(
            Map<String, SupportedLanguage> languageByCode,
            Map<String, SupportedLanguage> languageByName,
            Map<String, FrameworkVersion> frameworkVersionByCode,
            Map<String, FrameworkVersion> frameworkVersionByName,
            Map<String, RubricVersion> rubricVersionByCode,
            Map<String, RubricVersion> rubricVersionByName,
            Map<UUID, Rubric> rubricById,
            Map<String, SchoolClass> schoolClassByCode,
            Map<String, SchoolClass> schoolClassByName,
            Map<String, SchoolGrade> schoolGradeByCode,
            Map<String, SchoolGrade> schoolGradeByName,
            Map<String, SchoolGradeLevel> schoolGradeLevelByCode,
            Map<String, SchoolGradeLevel> schoolGradeLevelByName,
            Map<UUID, Map<String, FrameworkResultBand>> bandByVersionAndCode,
            Map<UUID, Map<String, FrameworkResultBand>> bandByVersionAndLabel) {}

    @Override
    public ImportCommitResult commit(ImportSession session, List<ImportRow> rows) {
        UUID schoolId = session.getSchoolId();
        boolean isSchoolScoped = schoolId != null;
        Map<String, String> mapping = resolveMapping(session);

        List<ImportRow> pendingRows = new ArrayList<>();
        List<Map<String, String>> mappedDataList = new ArrayList<>();
        for (ImportRow row : rows) {
            if (row.getStatus() != ImportRowStatus.PENDING) continue;
            Map<String, String> rawData = jsonSerializationPort.toStringMap(row.getRawDataJson());
            pendingRows.add(row);
            mappedDataList.add(applyMapping(mapping, rawData));
        }

        List<AssessmentPolicy> policiesToSave = new ArrayList<>();
        long importedCount = 0;
        long invalidCount = 0;
        OffsetDateTime now = OffsetDateTime.now();

        Set<ScopeKey> scopesClaimedInFile = new HashSet<>();
        Map<VersionScopeKey, Integer> nextVersionByScope = new HashMap<>();

        // Prefetch DUY NHẤT 1 lần toàn bộ Assessment Policy (mọi trạng thái) trong phạm vi schoolId này
        // (schoolId cố định suốt 1 lượt commit), rồi tự tính trong memory ở dưới thay vì gọi
        // existsActiveForScope/findMaxVersionForScope riêng lẻ cho từng dòng/từng scope trong file Excel.
        List<AssessmentPolicy> existingPolicies = assessmentPolicyRepository.findAllForOwner(schoolId);

        // Scope đang DRAFT/PUBLISHED -> dùng để chặn tạo trùng scope
        Set<ScopeKey> existingActiveScopes = existingPolicies.stream()
                .filter(p -> p.getStatus() == AssessmentPolicyStatus.DRAFT || p.getStatus() == AssessmentPolicyStatus.PUBLISHED)
                .map(p -> new ScopeKey(p.getSchoolId(), p.getLanguageId(), p.getFrameworkVersionId(),
                        p.getSchoolGradeLevelId(), p.getSchoolGradeId(), p.getSchoolClassId(), p.getRubricVersionId()))
                .collect(Collectors.toSet());

        // Version lớn nhất đã từng tồn tại theo từng scope (kể cả ARCHIVED) -> dùng để phát version kế tiếp,
        // đúng ý nghĩa gốc của AssessmentPolicyRepository#findMaxVersionForScope nhưng tính 1 lần trong memory.
        Map<VersionScopeKey, Integer> maxVersionByScope = existingPolicies.stream()
                .collect(Collectors.groupingBy(
                        p -> new VersionScopeKey(p.getSchoolId(), p.getLanguageId(), p.getFrameworkVersionId(),
                                p.getSchoolGradeLevelId(), p.getSchoolGradeId(), p.getSchoolClassId()),
                        Collectors.reducing(0, policy -> policy.getVersion(), (a, b) -> Integer.max(a, b))));

        // Prefetch toàn bộ dữ liệu tham chiếu (ngôn ngữ/khung/rubric/scope/band) bằng số query CỐ ĐỊNH,
        // rồi Phase 2 (loop chính) chỉ resolve trong memory, không còn gọi DB theo từng dòng/giá trị distinct nữa.
        LookupContext lookup = prefetchLookups(schoolId, isSchoolScoped, mappedDataList);

        for (int i = 0; i < pendingRows.size(); i++) {
            ImportRow row = pendingRows.get(i);
            Map<String, String> mappedData = mappedDataList.get(i);
            List<Map<String, String>> errors = new ArrayList<>();

            try {
                validateRequiredFields(mappedData, errors);

                UUID languageId = errors.isEmpty()
                        ? resolveLanguage(mappedData.get("language"), errors, lookup)
                        : null;

                FrameworkVersion frameworkVersion = errors.isEmpty()
                        ? resolveFrameworkVersion(mappedData.get("frameworkVersion"), errors, lookup)
                        : null;
                UUID frameworkVersionId = frameworkVersion != null ? frameworkVersion.getId() : null;

                UUID rubricVersionId = errors.isEmpty()
                        ? resolveRubricVersion(mappedData.get("rubricVersion"), frameworkVersion, schoolId, isSchoolScoped, errors, lookup)
                        : null;

                ScopeIds scopeIds = (errors.isEmpty() && isSchoolScoped)
                        ? resolveScope(mappedData.get("schoolClass"), mappedData.get("schoolGrade"), mappedData.get("schoolGradeLevel"), errors, lookup)
                        : ScopeIds.EMPTY;

                BandIds bandIds = errors.isEmpty()
                        ? resolveBands(frameworkVersionId, mappedData.get("targetFrameworkBand"), errors, lookup)
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
                    } else if (existingActiveScopes.contains(scopeKey)) {
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
                        maxVersionByScope.getOrDefault(key, 0) + 1);
                nextVersionByScope.put(versionScopeKey, nextVersion + 1);

                AssessmentPolicy newPolicy = new AssessmentPolicy(
                        schoolId, scopeIds.schoolGradeLevelId(), scopeIds.schoolGradeId(), scopeIds.schoolClassId(),
                        languageId, frameworkVersionId, rubricVersionId, bandIds.targetBandId(),
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

    // Gom toàn bộ input thô (distinct) của các dòng PENDING rồi query 1 lần/nhóm (findByCodeIn/findByNameIn)
    // thay vì query lười theo từng giá trị distinct trong loop chính (Phase 2). Band phụ thuộc frameworkVersionId
    // đã resolve (không phải input thô) nên gom theo FrameworkVersion tìm được rồi query findByFrameworkVersionIdIn.
    private LookupContext prefetchLookups(UUID schoolId, boolean isSchoolScoped, List<Map<String, String>> mappedDataList) {
        Set<String> languageInputs = new HashSet<>();
        Set<String> frameworkInputs = new HashSet<>();
        Set<String> rubricInputs = new HashSet<>();
        Set<String> classInputs = new HashSet<>();
        Set<String> gradeInputs = new HashSet<>();
        Set<String> gradeLevelInputs = new HashSet<>();

        for (Map<String, String> mappedData : mappedDataList) {
            addTrimmed(languageInputs, mappedData.get("language"));
            addTrimmed(frameworkInputs, mappedData.get("frameworkVersion"));
            addTrimmed(rubricInputs, mappedData.get("rubricVersion"));
            if (isSchoolScoped) {
                addTrimmed(classInputs, mappedData.get("schoolClass"));
                addTrimmed(gradeInputs, mappedData.get("schoolGrade"));
                addTrimmed(gradeLevelInputs, mappedData.get("schoolGradeLevel"));
            }
        }

        // *ByCode dùng key đã UPPERCASE (khớp với DB đã lọc UPPER() ở tầng Impl) để việc tra cứu không phân biệt
        // hoa/thường ("en"/"En"/"EN" đều khớp). *ByName giữ nguyên case gốc vì tên hiển thị không bị ép quy ước.
        Map<String, SupportedLanguage> languageByCode = indexBy(languageRepository.findByCodeIn(languageInputs), l -> l.getCode().value().toUpperCase());
        Map<String, SupportedLanguage> languageByName = indexBy(languageRepository.findByNameIn(languageInputs), language -> language.getName());

        Map<String, FrameworkVersion> frameworkVersionByCode = indexBy(frameworkVersionRepository.findByCodeIn(frameworkInputs), v -> v.getCode().toUpperCase());
        Map<String, FrameworkVersion> frameworkVersionByName = indexBy(frameworkVersionRepository.findByNameIn(frameworkInputs), version -> version.getName());

        Map<String, RubricVersion> rubricVersionByCode = indexBy(rubricVersionRepository.findByCodeIn(rubricInputs), v -> v.getCode().toUpperCase());
        Map<String, RubricVersion> rubricVersionByName = indexBy(rubricVersionRepository.findByNameIn(rubricInputs), version -> version.getName());

        Set<UUID> rubricIds = new HashSet<>();
        rubricVersionByCode.values().forEach(v -> rubricIds.add(v.getRubricId()));
        rubricVersionByName.values().forEach(v -> rubricIds.add(v.getRubricId()));
        Map<UUID, Rubric> rubricById = indexBy(rubricRepository.findByIdIn(rubricIds), r -> r.getId());

        Map<String, SchoolClass> schoolClassByCode = Map.of();
        Map<String, SchoolClass> schoolClassByName = Map.of();
        Map<String, SchoolGrade> schoolGradeByCode = Map.of();
        Map<String, SchoolGrade> schoolGradeByName = Map.of();
        Map<String, SchoolGradeLevel> schoolGradeLevelByCode = Map.of();
        Map<String, SchoolGradeLevel> schoolGradeLevelByName = Map.of();
        if (isSchoolScoped) {
            schoolClassByCode = indexBy(schoolClassRepository.findBySchoolIdAndCodeIn(schoolId, classInputs), c -> c.getCode().value().toUpperCase());
            schoolClassByName = indexBy(schoolClassRepository.findBySchoolIdAndNameIn(schoolId, classInputs), c -> c.getName());
            schoolGradeByCode = indexBy(schoolGradeRepository.findBySchoolIdAndCodeIn(schoolId, gradeInputs), g -> g.getCode().toUpperCase());
            schoolGradeByName = indexBy(schoolGradeRepository.findBySchoolIdAndNameIn(schoolId, gradeInputs), g -> g.getName());
            schoolGradeLevelByCode = indexBy(schoolGradeLevelRepository.findBySchoolIdAndCodeIn(schoolId, gradeLevelInputs), l -> l.getCode().toUpperCase());
            schoolGradeLevelByName = indexBy(schoolGradeLevelRepository.findBySchoolIdAndNameIn(schoolId, gradeLevelInputs), level -> level.getName());
        }

        Set<UUID> frameworkVersionIds = new HashSet<>();
        frameworkVersionByCode.values().forEach(v -> frameworkVersionIds.add(v.getId()));
        frameworkVersionByName.values().forEach(v -> frameworkVersionIds.add(v.getId()));
        List<FrameworkResultBand> bands = frameworkVersionIds.isEmpty()
                ? List.of()
                : frameworkResultBandRepository.findByFrameworkVersionIdIn(frameworkVersionIds);
        Map<UUID, Map<String, FrameworkResultBand>> bandByVersionAndCode = new HashMap<>();
        Map<UUID, Map<String, FrameworkResultBand>> bandByVersionAndLabel = new HashMap<>();
        for (FrameworkResultBand band : bands) {
            bandByVersionAndCode.computeIfAbsent(band.getFrameworkVersionId(), k -> new HashMap<>()).put(band.getCode().toUpperCase(), band);
            bandByVersionAndLabel.computeIfAbsent(band.getFrameworkVersionId(), k -> new HashMap<>()).put(band.getLabel(), band);
        }

        return new LookupContext(languageByCode, languageByName, frameworkVersionByCode, frameworkVersionByName,
                rubricVersionByCode, rubricVersionByName, rubricById,
                schoolClassByCode, schoolClassByName, schoolGradeByCode, schoolGradeByName,
                schoolGradeLevelByCode, schoolGradeLevelByName, bandByVersionAndCode, bandByVersionAndLabel);
    }

    private static void addTrimmed(Set<String> set, String value) {
        if (value != null && !value.isBlank()) set.add(value.trim());
    }

    private static <T, K> Map<K, T> indexBy(List<T> items, Function<T, K> keyFn) {
        Map<K, T> map = new HashMap<>();
        for (T item : items) {
            map.put(keyFn.apply(item), item);
        }
        return map;
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
        requireField(mappedData.get("effectiveFrom"), "effectiveFrom", "Thiếu Ngày bắt đầu.", errors);
    }

    private static void requireField(String value, String field, String errorMessage, List<Map<String, String>> errors) {
        if (value == null || value.isBlank()) errors.add(error(field, errorMessage));
    }

    // Dual Lookup: cho phép người dùng điền mã HOẶC tên trong file Excel. Tra thuần trong memory (đã prefetch).
    private static UUID resolveLanguage(String languageInput, List<Map<String, String>> errors, LookupContext lookup) {
        String cleanInput = languageInput.trim();
        SupportedLanguage language = lookup.languageByCode().get(cleanInput.toUpperCase());
        if (language == null) language = lookup.languageByName().get(cleanInput);
        if (language == null) {
            errors.add(error("language", "Ngôn ngữ '" + cleanInput + "' không tồn tại trên hệ thống."));
            return null;
        }
        return language.getId();
    }

    private static FrameworkVersion resolveFrameworkVersion(String fwVersionInput, List<Map<String, String>> errors, LookupContext lookup) {
        String cleanInput = fwVersionInput.trim();
        FrameworkVersion frameworkVersion = lookup.frameworkVersionByCode().get(cleanInput.toUpperCase());
        if (frameworkVersion == null) frameworkVersion = lookup.frameworkVersionByName().get(cleanInput);
        if (frameworkVersion == null) {
            errors.add(error("frameworkVersion", "Không tìm thấy Phiên bản Khung: '" + cleanInput + "'"));
            return null;
        }
        if (frameworkVersion.getStatus() != FrameworkVersionStatus.PUBLISHED) {
            errors.add(error("frameworkVersion", "Phiên bản Khung này chưa được PUBLISHED."));
            return null;
        }
        return frameworkVersion;
    }

    private static UUID resolveRubricVersion(String rubricVersionInput, FrameworkVersion frameworkVersion, UUID schoolId,
            boolean isSchoolScoped, List<Map<String, String>> errors, LookupContext lookup) {
        String cleanInput = rubricVersionInput.trim();
        RubricVersion rubricVersion = lookup.rubricVersionByCode().get(cleanInput.toUpperCase());
        if (rubricVersion == null) rubricVersion = lookup.rubricVersionByName().get(cleanInput);
        if (rubricVersion == null) {
            errors.add(error("rubricVersion", "Không tìm thấy Phiên bản Rubric: '" + cleanInput + "'"));
            return null;
        }

        if (rubricVersion.getStatus() == RubricStatus.PUBLISHED) {
            errors.add(error("rubricVersion", "Phiên bản Rubric này đã được PUBLISHED, không thể tạo Assessment Policy mới cho phiên bản này."));
            return null;
        }
        Rubric rubric = lookup.rubricById().get(rubricVersion.getRubricId());
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

    // Phải điền ít nhất 1 trong 3: Lớp, Khối năm học (schoolGrade), Khối (schoolGradeLevel).
    // Nếu điền nhiều hơn 1, ưu tiên theo thứ tự: Khối (schoolGradeLevel) -> Khối năm học (schoolGrade) -> Lớp (schoolClass)
    // — khác với CreateSchoolAssessmentPolicyUseCase (tạo tay vẫn bắt buộc chọn CHÍNH XÁC 1, không có fallback này).
    private static ScopeIds resolveScope(String classInput, String gradeInput, String gradeLevelInput,
            List<Map<String, String>> errors, LookupContext lookup) {
        boolean hasClass = classInput != null && !classInput.isBlank();
        boolean hasGrade = gradeInput != null && !gradeInput.isBlank();
        boolean hasGradeLevel = gradeLevelInput != null && !gradeLevelInput.isBlank();

        if (!hasClass && !hasGrade && !hasGradeLevel) {
            errors.add(error("scope", "Phải điền ít nhất 1 phạm vi áp dụng: Lớp, Khối năm học, HOẶC Khối."));
            return ScopeIds.EMPTY;
        }

        if (hasGradeLevel) {
            String cleanInput = gradeLevelInput == null ? "" : gradeLevelInput.trim();
            SchoolGradeLevel schoolGradeLevel = lookup.schoolGradeLevelByCode().get(cleanInput.toUpperCase());
            if (schoolGradeLevel == null) schoolGradeLevel = lookup.schoolGradeLevelByName().get(cleanInput);
            if (schoolGradeLevel == null) {
                errors.add(error("schoolGradeLevel", "Không tìm thấy Khối '" + cleanInput + "'."));
                return ScopeIds.EMPTY;
            }
            return new ScopeIds(null, null, schoolGradeLevel.getId());
        }

        if (hasGrade) {
            String cleanInput = gradeInput == null ? "" : gradeInput.trim();
            SchoolGrade schoolGrade = lookup.schoolGradeByCode().get(cleanInput.toUpperCase());
            if (schoolGrade == null) schoolGrade = lookup.schoolGradeByName().get(cleanInput);
            if (schoolGrade == null) {
                errors.add(error("schoolGrade", "Không tìm thấy Khối năm học '" + cleanInput + "'."));
                return ScopeIds.EMPTY;
            }
            return new ScopeIds(null, schoolGrade.getId(), null);
        }

        String cleanInput = classInput == null ? "" : classInput.trim();
        SchoolClass schoolClass = lookup.schoolClassByCode().get(cleanInput.toUpperCase());
        if (schoolClass == null) schoolClass = lookup.schoolClassByName().get(cleanInput);
        if (schoolClass == null) {
            errors.add(error("schoolClass", "Không tìm thấy Lớp '" + cleanInput + "'."));
            return ScopeIds.EMPTY;
        }
        return new ScopeIds(schoolClass.getId(), null, null);
    }

    private static BandIds resolveBands(UUID frameworkVersionId, String targetBandInput,
            List<Map<String, String>> errors, LookupContext lookup) {
        String cleanTarget = targetBandInput.trim();
        FrameworkResultBand targetBand = findBand(frameworkVersionId, cleanTarget, lookup);

        if (targetBand == null) errors.add(error("targetFrameworkBand", "Band mục tiêu '" + cleanTarget + "' không tồn tại trong Khung này."));

        return new BandIds(targetBand != null ? targetBand.getId() : null);
    }

    private static FrameworkResultBand findBand(UUID frameworkVersionId, String cleanInput, LookupContext lookup) {
        Map<String, FrameworkResultBand> byCode = lookup.bandByVersionAndCode().get(frameworkVersionId);
        FrameworkResultBand band = byCode != null ? byCode.get(cleanInput.toUpperCase()) : null;
        if (band == null) {
            Map<String, FrameworkResultBand> byLabel = lookup.bandByVersionAndLabel().get(frameworkVersionId);
            band = byLabel != null ? byLabel.get(cleanInput) : null;
        }
        return band;
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