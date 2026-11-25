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
import com.sep.vox.domain.model.gradelevel.GradeLevel;
import com.sep.vox.domain.model.supportedlanguage.SupportedLanguage;
import com.sep.vox.domain.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AssessmentPolicyImportCommitHandler implements ImportCommitHandler {

    private final AssessmentPolicyRepository assessmentPolicyRepository;
    private final FrameworkVersionRepository frameworkVersionRepository;
    private final FrameworkResultBandRepository frameworkResultBandRepository;
    private final RubricVersionRepository rubricVersionRepository;
    private final RubricRepository rubricRepository;
    private final SupportedLanguageRepository languageRepository;
    private final GradeLevelRepository gradeLevelRepository;
    private final SchoolGradeRepository schoolGradeRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final JsonSerializationPort jsonSerializationPort;
    private final GradeLevelBandScopeGuardService gradeLevelBandScopeGuardService;

    @Override
    public ImportType supportedType() {
        return ImportType.ASSESSMENT_POLICY;
    }
    // gradeLevelId: Khối (tĩnh, không gắn năm học) | schoolGradeId: Khối năm học (khối trong 1 năm học cụ thể)
    // KHÔNG gồm rubricVersionId: một phạm vi chỉ được đúng một chính sách còn hiệu lực, bất kể
    // trỏ vào phiên bản Rubric nào (siết 2026-08-14, giống CreateSchoolAssessmentPolicyUseCase).
    // Trước đó trùng scope mà khác Rubric Version vẫn lọt, sinh ra chính sách không bao giờ được dùng.
    private record ScopeKey(UUID schoolId, UUID languageId, UUID frameworkVersionId,
                            UUID gradeLevelId, UUID schoolGradeId, UUID schoolClassId) {}

    // Không gồm rubricVersionId: dùng để phát số "version" kế tiếp, vì DB chỉ unique theo scope+version
    // (không phân biệt Rubric Version), nên các Policy khác Rubric Version nhưng cùng scope này vẫn phải chiếm
    // các số version khác nhau.
    private record VersionScopeKey(UUID schoolId, UUID languageId, UUID frameworkVersionId,
                            UUID gradeLevelId, UUID schoolGradeId, UUID schoolClassId) {}

    // Đúng 1 trong 3 phạm vi được điền: Lớp, Khối năm học, hoặc Khối
    private record ScopeIds(UUID schoolClassId, UUID schoolGradeId, UUID gradeLevelId) {
        static final ScopeIds EMPTY = new ScopeIds(null, null, null);
    }

    // Giữ cả object Band chứ không chỉ id: bước kiểm trần bậc theo Khối cần result_band_order,
    // mà band đã nằm sẵn trong LookupContext nên mang theo rẻ hơn là tra lại DB.
    private record BandIds(UUID targetBandId, FrameworkResultBand targetBand) {
        static final BandIds EMPTY = new BandIds(null, null);
    }

    private record EffectivePeriod(Instant from, Instant to) {
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
            Map<String, GradeLevel> schoolGradeLevelByCode,
            Map<String, GradeLevel> schoolGradeLevelByName,
            Map<UUID, Map<String, FrameworkResultBand>> bandByVersionAndCode,
            Map<UUID, Map<String, FrameworkResultBand>> bandByVersionAndLabel) {}

    @Override
    public ImportCommitResult commit(ImportSession session, List<ImportRow> rows) {
        UUID schoolId = session.getSchoolId();
        PendingRows pending = collectPendingRows(rows, resolveMapping(session));
        CommitRun run = buildCommitRun(schoolId, pending.mappedDataList());

        List<AssessmentPolicy> policiesToSave = new ArrayList<>();
        long importedCount = 0;
        long invalidCount = 0;

        for (int i = 0; i < pending.rows().size(); i++) {
            if (commitRow(pending.rows().get(i), pending.mappedDataList().get(i), run, policiesToSave)) {
                importedCount++;
            } else {
                invalidCount++;
            }
        }

        if (!policiesToSave.isEmpty()) {
            assessmentPolicyRepository.saveAll(policiesToSave);
        }

        return new ImportCommitResult(importedCount, 0, 0, invalidCount);
    }

    private record PendingRows(List<ImportRow> rows, List<Map<String, String>> mappedDataList) {}

    private PendingRows collectPendingRows(List<ImportRow> rows, Map<String, String> mapping) {
        List<ImportRow> pendingRows = new ArrayList<>();
        List<Map<String, String>> mappedDataList = new ArrayList<>();
        for (ImportRow row : rows) {
            if (row.getStatus() != ImportRowStatus.PENDING) continue;
            Map<String, String> rawData = jsonSerializationPort.toStringMap(row.getRawDataJson());
            pendingRows.add(row);
            mappedDataList.add(applyMapping(mapping, rawData));
        }
        return new PendingRows(pendingRows, mappedDataList);
    }

    private record CommitRun(
            UUID schoolId,
            boolean isSchoolScoped,
            LookupContext lookup,
            GradeLevelBandScopeGuardService.Batch bandScopeBatch,
            Set<ScopeKey> scopesClaimedInFile,
            Map<VersionScopeKey, Integer> nextVersionByScope,
            Set<ScopeKey> existingActiveScopes,
            Map<VersionScopeKey, Integer> maxVersionByScope) {}

    // Fetches EVERY Assessment Policy (any status) of this schoolId ONCE (schoolId is fixed for the whole
    // commit) and computes in memory, instead of calling existsActiveForScope/findMaxVersionForScope per
    // row/scope of the Excel file. The reference data (language/framework/rubric/scope/band) is prefetched
    // with a FIXED number of queries, so the main loop only resolves in memory and never calls the DB per row.
    // The band ceiling is not in LookupContext because it looks up the grade level DERIVED from each row's
    // scope, unknown at prefetch time. The batch remembers per distinct value, so it is still not N+1.
    private CommitRun buildCommitRun(UUID schoolId, List<Map<String, String>> mappedDataList) {
        boolean isSchoolScoped = schoolId != null;
        List<AssessmentPolicy> existingPolicies = assessmentPolicyRepository.findAllForOwner(schoolId);
        LookupContext lookup = prefetchLookups(schoolId, isSchoolScoped, mappedDataList);
        return new CommitRun(schoolId, isSchoolScoped, lookup, gradeLevelBandScopeGuardService.newBatch(),
                new HashSet<>(), new HashMap<>(), collectActiveScopes(existingPolicies),
                collectMaxVersionByScope(existingPolicies));
    }

    // Scopes in DRAFT/PUBLISHED -> used to block creating a duplicate scope
    private Set<ScopeKey> collectActiveScopes(List<AssessmentPolicy> existingPolicies) {
        return existingPolicies.stream()
                .filter(p -> p.getStatus() == AssessmentPolicyStatus.DRAFT || p.getStatus() == AssessmentPolicyStatus.PUBLISHED)
                .map(p -> new ScopeKey(p.getSchoolId(), p.getLanguageId(), p.getFrameworkVersionId(),
                        p.getGradeLevelId(), p.getSchoolGradeId(), p.getSchoolClassId()))
                .collect(Collectors.toSet());
    }

    // Highest version that ever existed per scope (ARCHIVED included) -> used to issue the next version,
    // the original meaning of AssessmentPolicyRepository#findMaxVersionForScope but computed once in memory.
    private Map<VersionScopeKey, Integer> collectMaxVersionByScope(List<AssessmentPolicy> existingPolicies) {
        return existingPolicies.stream()
                .collect(Collectors.groupingBy(
                        p -> new VersionScopeKey(p.getSchoolId(), p.getLanguageId(), p.getFrameworkVersionId(),
                                p.getGradeLevelId(), p.getSchoolGradeId(), p.getSchoolClassId()),
                        Collectors.reducing(0, policy -> policy.getVersion(), (a, b) -> Integer.max(a, b))));
    }

    /** Returns true when the row was imported, false when it was marked invalid. */
    private boolean commitRow(ImportRow row, Map<String, String> mappedData, CommitRun run,
            List<AssessmentPolicy> policiesToSave) {
        List<Map<String, String>> errors = new ArrayList<>();
        try {
            ResolvedReferences references = resolveReferences(mappedData, run, errors);
            ParsedValues values = parseValues(mappedData, errors);
            claimScope(run, references, errors);

            if (!errors.isEmpty()) {
                markInvalid(row, errors);
                return false;
            }

            policiesToSave.add(buildPolicy(run, references, values));
            row.setStatus(ImportRowStatus.IMPORTED);
            row.setMappedDataJson(jsonSerializationPort.toJson(mappedData));
            return true;
        } catch (Exception ex) {
            errors.add(error("general", "Lỗi xử lý luồng ngầm: " + ex.getMessage()));
            markInvalid(row, errors);
            return false;
        }
    }

    private void markInvalid(ImportRow row, List<Map<String, String>> errors) {
        row.setStatus(ImportRowStatus.INVALID);
        row.setErrorsJson(jsonSerializationPort.toJson(errors));
    }

    private record ResolvedReferences(UUID languageId, UUID frameworkVersionId, UUID rubricVersionId,
                                      ScopeIds scopeIds, BandIds bandIds) {}

    private record ParsedValues(BigDecimal passingScore, AssessmentPolicyStrictness strictness,
                                EffectivePeriod effectivePeriod) {}

    private ResolvedReferences resolveReferences(Map<String, String> mappedData, CommitRun run,
            List<Map<String, String>> errors) {
        validateRequiredFields(mappedData, errors);
        UUID languageId = errors.isEmpty() ? resolveLanguage(mappedData.get("language"), errors, run.lookup()) : null;
        FrameworkVersion frameworkVersion = errors.isEmpty()
                ? resolveFrameworkVersion(mappedData.get("frameworkVersion"), errors, run.lookup()) : null;
        UUID frameworkVersionId = frameworkVersion != null ? frameworkVersion.getId() : null;
        UUID rubricVersionId = errors.isEmpty() ? resolveRubricVersion(mappedData.get("rubricVersion"),
                frameworkVersion, run.schoolId(), run.isSchoolScoped(), errors, run.lookup()) : null;
        ScopeIds scopeIds = (errors.isEmpty() && run.isSchoolScoped()) ? resolveScope(mappedData.get("schoolClass"),
                mappedData.get("schoolGrade"), mappedData.get("schoolGradeLevel"), errors, run.lookup()) : ScopeIds.EMPTY;
        BandIds bandIds = errors.isEmpty()
                ? resolveBands(frameworkVersionId, mappedData.get("targetFrameworkBand"), errors, run.lookup())
                : BandIds.EMPTY;
        assertBandWithinGradeScope(run, scopeIds, frameworkVersionId, bandIds, errors);
        return new ResolvedReferences(languageId, frameworkVersionId, rubricVersionId, scopeIds, bandIds);
    }

    // Band ceiling per grade level. The guard throws IllegalArgumentException for the API flow, but here
    // a bad row is only marked INVALID -- it must not bring down the whole import file.
    private void assertBandWithinGradeScope(CommitRun run, ScopeIds scopeIds, UUID frameworkVersionId,
            BandIds bandIds, List<Map<String, String>> errors) {
        if (errors.isEmpty() && run.isSchoolScoped() && bandIds.targetBand() != null) {
            try {
                run.bandScopeBatch().assertWithinScope(
                        scopeIds.gradeLevelId(), scopeIds.schoolGradeId(), scopeIds.schoolClassId(),
                        frameworkVersionId, bandIds.targetBand());
            } catch (IllegalArgumentException e) {
                errors.add(error("targetFrameworkBand", e.getMessage()));
            }
        }
    }

    private ParsedValues parseValues(Map<String, String> mappedData, List<Map<String, String>> errors) {
        BigDecimal passingScore = errors.isEmpty()
                ? parsePassingScore(mappedData.get("passingScore"), errors)
                : null;
        AssessmentPolicyStrictness strictness = errors.isEmpty()
                ? parseStrictness(mappedData.get("strictness"), errors)
                : AssessmentPolicyStrictness.STANDARD;
        EffectivePeriod effectivePeriod = errors.isEmpty()
                ? parseEffectivePeriod(mappedData.get("effectiveFrom"), mappedData.get("effectiveTo"), errors)
                : EffectivePeriod.EMPTY;
        return new ParsedValues(passingScore, strictness, effectivePeriod);
    }

    // Reusing one Rubric Version in several rows is VALID since V44: many different classes are graded
    // with the same set of criteria. The constraint still in force is a duplicate scope, checked here.
    private void claimScope(CommitRun run, ResolvedReferences references, List<Map<String, String>> errors) {
        if (!errors.isEmpty()) {
            return;
        }
        ScopeKey scopeKey = new ScopeKey(run.schoolId(), references.languageId(), references.frameworkVersionId(),
                references.scopeIds().gradeLevelId(), references.scopeIds().schoolGradeId(),
                references.scopeIds().schoolClassId());
        if (!run.scopesClaimedInFile().add(scopeKey)) {
            errors.add(error("scope", "Bị trùng phạm vi áp dụng ngay trong file Excel."));
        } else if (run.existingActiveScopes().contains(scopeKey)) {
            errors.add(error("scope", "Đã tồn tại một Quy chế (DRAFT/PUBLISHED) khác cho cùng phạm vi này. Vui lòng lưu trữ (ARCHIVE) bản cũ."));
        }
    }

    private AssessmentPolicy buildPolicy(CommitRun run, ResolvedReferences references, ParsedValues values) {
        ScopeIds scopeIds = references.scopeIds();
        VersionScopeKey versionScopeKey = new VersionScopeKey(run.schoolId(), references.languageId(),
                references.frameworkVersionId(), scopeIds.gradeLevelId(), scopeIds.schoolGradeId(),
                scopeIds.schoolClassId());
        int nextVersion = run.nextVersionByScope().computeIfAbsent(versionScopeKey, key ->
                run.maxVersionByScope().getOrDefault(key, 0) + 1);
        run.nextVersionByScope().put(versionScopeKey, nextVersion + 1);

        return AssessmentPolicy.draftForScope(run.schoolId(), scopeIds.gradeLevelId(),
            scopeIds.schoolGradeId(), scopeIds.schoolClassId(), references.languageId(),
            references.frameworkVersionId(), references.rubricVersionId(),
            references.bandIds().targetBandId(), values.passingScore(), values.strictness(), nextVersion,
            values.effectivePeriod().from(), values.effectivePeriod().to());
    }

    // Collects every distinct raw input of the PENDING rows and queries once per group (findByCodeIn/findByNameIn)
    // instead of lazy per-distinct-value queries in the main loop (Phase 2). Bands depend on the already
    // resolved frameworkVersionId (not a raw input), so they are grouped by the FrameworkVersions found and
    // queried with findByFrameworkVersionIdIn.
    private LookupContext prefetchLookups(UUID schoolId, boolean isSchoolScoped, List<Map<String, String>> mappedDataList) {
        LookupInputs inputs = collectLookupInputs(isSchoolScoped, mappedDataList);

        // *ByCode uses UPPERCASE keys (matching the DB, which filters with UPPER() in the Impl layer) so the
        // lookup is case-insensitive ("en"/"En"/"EN" all match). *ByName keeps the original case because
        // display names are not forced into a convention.
        Map<String, SupportedLanguage> languageByCode = indexBy(languageRepository.findByCodeIn(inputs.languages()), l -> l.getCode().value().toUpperCase());
        Map<String, SupportedLanguage> languageByName = indexBy(languageRepository.findByNameIn(inputs.languages()), language -> language.getName());
        Map<String, FrameworkVersion> frameworkVersionByCode = indexBy(frameworkVersionRepository.findByCodeIn(inputs.frameworks()), v -> v.getCode().toUpperCase());
        Map<String, FrameworkVersion> frameworkVersionByName = indexBy(frameworkVersionRepository.findByNameIn(inputs.frameworks()), version -> version.getName());
        Map<String, RubricVersion> rubricVersionByCode = indexBy(rubricVersionRepository.findByCodeIn(inputs.rubrics()), v -> v.getCode().toUpperCase());
        Map<String, RubricVersion> rubricVersionByName = indexBy(rubricVersionRepository.findByNameIn(inputs.rubrics()), version -> version.getName());
        Map<UUID, Rubric> rubricById = loadRubricsOfVersions(rubricVersionByCode, rubricVersionByName);
        SchoolScopeLookups schoolScope = isSchoolScoped ? loadSchoolScopeLookups(schoolId, inputs) : SchoolScopeLookups.EMPTY;
        BandLookups bands = loadBandLookups(frameworkVersionByCode, frameworkVersionByName);

        return new LookupContext(languageByCode, languageByName, frameworkVersionByCode, frameworkVersionByName,
                rubricVersionByCode, rubricVersionByName, rubricById,
                schoolScope.classByCode(), schoolScope.classByName(), schoolScope.gradeByCode(), schoolScope.gradeByName(),
                schoolScope.gradeLevelByCode(), schoolScope.gradeLevelByName(), bands.byVersionAndCode(), bands.byVersionAndLabel());
    }

    private record LookupInputs(Set<String> languages, Set<String> frameworks, Set<String> rubrics,
                                Set<String> classes, Set<String> grades, Set<String> gradeLevels) {}

    private LookupInputs collectLookupInputs(boolean isSchoolScoped, List<Map<String, String>> mappedDataList) {
        LookupInputs inputs = new LookupInputs(new HashSet<>(), new HashSet<>(), new HashSet<>(),
                new HashSet<>(), new HashSet<>(), new HashSet<>());
        for (Map<String, String> mappedData : mappedDataList) {
            addTrimmed(inputs.languages(), mappedData.get("language"));
            addTrimmed(inputs.frameworks(), mappedData.get("frameworkVersion"));
            addTrimmed(inputs.rubrics(), mappedData.get("rubricVersion"));
            if (isSchoolScoped) {
                addTrimmed(inputs.classes(), mappedData.get("schoolClass"));
                addTrimmed(inputs.grades(), mappedData.get("schoolGrade"));
                addTrimmed(inputs.gradeLevels(), mappedData.get("schoolGradeLevel"));
            }
        }
        return inputs;
    }

    private Map<UUID, Rubric> loadRubricsOfVersions(Map<String, RubricVersion> rubricVersionByCode,
            Map<String, RubricVersion> rubricVersionByName) {
        Set<UUID> rubricIds = new HashSet<>();
        rubricVersionByCode.values().forEach(v -> rubricIds.add(v.getRubricId()));
        rubricVersionByName.values().forEach(v -> rubricIds.add(v.getRubricId()));
        return indexBy(rubricRepository.findByIdIn(rubricIds), r -> r.getId());
    }

    private record SchoolScopeLookups(
            Map<String, SchoolClass> classByCode,
            Map<String, SchoolClass> classByName,
            Map<String, SchoolGrade> gradeByCode,
            Map<String, SchoolGrade> gradeByName,
            Map<String, GradeLevel> gradeLevelByCode,
            Map<String, GradeLevel> gradeLevelByName) {
        static final SchoolScopeLookups EMPTY = new SchoolScopeLookups(
                Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of());
    }

    private SchoolScopeLookups loadSchoolScopeLookups(UUID schoolId, LookupInputs inputs) {
        return new SchoolScopeLookups(
                indexBy(schoolClassRepository.findBySchoolIdAndCodeIn(schoolId, inputs.classes()), c -> c.getCode().value().toUpperCase()),
                indexBy(schoolClassRepository.findBySchoolIdAndNameIn(schoolId, inputs.classes()), c -> c.getName()),
                indexBy(schoolGradeRepository.findBySchoolIdAndCodeIn(schoolId, inputs.grades()), g -> g.getCode().toUpperCase()),
                indexBy(schoolGradeRepository.findBySchoolIdAndNameIn(schoolId, inputs.grades()), g -> g.getName()),
                indexBy(gradeLevelRepository.findByCodeIn(inputs.gradeLevels()), l -> l.getCode().toUpperCase()),
                indexBy(gradeLevelRepository.findByNameIn(inputs.gradeLevels()), level -> level.getName()));
    }

    private record BandLookups(
            Map<UUID, Map<String, FrameworkResultBand>> byVersionAndCode,
            Map<UUID, Map<String, FrameworkResultBand>> byVersionAndLabel) {}

    private BandLookups loadBandLookups(Map<String, FrameworkVersion> frameworkVersionByCode,
            Map<String, FrameworkVersion> frameworkVersionByName) {
        Set<UUID> frameworkVersionIds = new HashSet<>();
        frameworkVersionByCode.values().forEach(v -> frameworkVersionIds.add(v.getId()));
        frameworkVersionByName.values().forEach(v -> frameworkVersionIds.add(v.getId()));
        List<FrameworkResultBand> bands = frameworkVersionIds.isEmpty()
                ? List.of()
                : frameworkResultBandRepository.findByFrameworkVersionIdIn(frameworkVersionIds);
        Map<UUID, Map<String, FrameworkResultBand>> byVersionAndCode = new HashMap<>();
        Map<UUID, Map<String, FrameworkResultBand>> byVersionAndLabel = new HashMap<>();
        for (FrameworkResultBand band : bands) {
            byVersionAndCode.computeIfAbsent(band.getFrameworkVersionId(), k -> new HashMap<>()).put(band.getCode().toUpperCase(), band);
            byVersionAndLabel.computeIfAbsent(band.getFrameworkVersionId(), k -> new HashMap<>()).put(band.getLabel(), band);
        }
        return new BandLookups(byVersionAndCode, byVersionAndLabel);
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

        // Same rule as the two manual-create use cases (Create{School,System}AssessmentPolicyUseCase): block
        // ARCHIVED but not PUBLISHED. An import stricter than the manual path would make the same Excel file
        // sometimes work and sometimes not, depending on whether the Rubric version has been published yet.
        if (rubricVersion.getStatus() == RubricStatus.ARCHIVED) {
            errors.add(error("rubricVersion", "Phiên bản Rubric này đã được lưu trữ (ARCHIVED), không thể tạo Assessment Policy mới cho phiên bản này."));
            return null;
        }
        addRubricOwnershipErrors(lookup.rubricById().get(rubricVersion.getRubricId()), frameworkVersion, schoolId,
                isSchoolScoped, errors);
        return rubricVersion.getId();
    }

    private static void addRubricOwnershipErrors(Rubric rubric, FrameworkVersion frameworkVersion, UUID schoolId,
            boolean isSchoolScoped, List<Map<String, String>> errors) {
        if (rubric == null) {
            errors.add(error("rubricVersion", "Không tìm thấy Rubric gốc."));
        } else if (isSchoolScoped) {
            addSchoolRubricErrors(rubric, frameworkVersion, schoolId, errors);
        } else {
            addSystemRubricErrors(rubric, frameworkVersion, errors);
        }
    }

    private static void addSchoolRubricErrors(Rubric rubric, FrameworkVersion frameworkVersion, UUID schoolId,
            List<Map<String, String>> errors) {
        if (rubric.getOwnerType() != RubricOwnerType.SCHOOL || !schoolId.equals(rubric.getSchoolId())) {
            errors.add(error("rubricVersion", "Rubric Version không thuộc quyền quản lý của trường bạn."));
        } else if (!rubric.getFrameworkId().equals(frameworkVersion.getFrameworkId())) {
            errors.add(error("rubricVersion", "Rubric và Khung năng lực không khớp nhau."));
        }
    }

    private static void addSystemRubricErrors(Rubric rubric, FrameworkVersion frameworkVersion,
            List<Map<String, String>> errors) {
        if (rubric.getOwnerType() != RubricOwnerType.SYSTEM) {
            errors.add(error("rubricVersion", "Chỉ được dùng Rubric SYSTEM cho luồng System Admin."));
        } else if (!rubric.getFrameworkId().equals(frameworkVersion.getFrameworkId())) {
            errors.add(error("rubricVersion", "Rubric và Khung năng lực không khớp nhau."));
        }
    }

    // At least 1 of the 3 must be filled: Class, school grade (schoolGrade), Grade level (schoolGradeLevel).
    // If more than one is filled, the priority is: Grade level (schoolGradeLevel) -> school grade (schoolGrade)
    // -> Class (schoolClass) -- unlike CreateSchoolAssessmentPolicyUseCase (manual creation still requires
    // EXACTLY 1, with no such fallback).
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
            return resolveGradeLevelScope(gradeLevelInput, errors, lookup);
        }
        if (hasGrade) {
            return resolveSchoolGradeScope(gradeInput, errors, lookup);
        }
        return resolveSchoolClassScope(classInput, errors, lookup);
    }

    private static ScopeIds resolveGradeLevelScope(String gradeLevelInput, List<Map<String, String>> errors,
            LookupContext lookup) {
        String cleanInput = gradeLevelInput == null ? "" : gradeLevelInput.trim();
        GradeLevel schoolGradeLevel = lookup.schoolGradeLevelByCode().get(cleanInput.toUpperCase());
        if (schoolGradeLevel == null) schoolGradeLevel = lookup.schoolGradeLevelByName().get(cleanInput);
        if (schoolGradeLevel == null) {
            errors.add(error("schoolGradeLevel", "Không tìm thấy Khối '" + cleanInput + "'."));
            return ScopeIds.EMPTY;
        }
        return new ScopeIds(null, null, schoolGradeLevel.getId());
    }

    private static ScopeIds resolveSchoolGradeScope(String gradeInput, List<Map<String, String>> errors,
            LookupContext lookup) {
        String cleanInput = gradeInput == null ? "" : gradeInput.trim();
        SchoolGrade schoolGrade = lookup.schoolGradeByCode().get(cleanInput.toUpperCase());
        if (schoolGrade == null) schoolGrade = lookup.schoolGradeByName().get(cleanInput);
        if (schoolGrade == null) {
            errors.add(error("schoolGrade", "Không tìm thấy Khối năm học '" + cleanInput + "'."));
            return ScopeIds.EMPTY;
        }
        return new ScopeIds(null, schoolGrade.getId(), null);
    }

    private static ScopeIds resolveSchoolClassScope(String classInput, List<Map<String, String>> errors,
            LookupContext lookup) {
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

        return new BandIds(targetBand != null ? targetBand.getId() : null, targetBand);
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
            Instant effectiveFrom = DateMapper.toImportedInstant(
                    effectiveFromStr.trim(), DateMapper.DEFAULT_INPUT_ZONE);
            Instant effectiveTo = (effectiveToStr != null && !effectiveToStr.isBlank())
                    ? DateMapper.toImportedInstant(effectiveToStr.trim(), DateMapper.DEFAULT_INPUT_ZONE)
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