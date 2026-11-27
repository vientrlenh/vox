package com.sep.vox.domain.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicy;

public interface AssessmentPolicyRepository {
    Optional<AssessmentPolicy> findById(UUID id);
    AssessmentPolicy save(AssessmentPolicy policy);
    List<AssessmentPolicy> saveAll(List<AssessmentPolicy> policies);

    // List of system-wide Assessment Policies (schoolId IS NULL)
    PageResult<AssessmentPolicy> findAllSystemWide(String status, UUID languageId, UUID rubricVersionId,
            Instant effectiveFrom, Instant effectiveTo, int page, int size);
    // List of Assessment Policies of a school
    PageResult<AssessmentPolicy> findAllBySchoolId(UUID schoolId, String status, UUID languageId, UUID rubricVersionId,
            Instant effectiveFrom, Instant effectiveTo, int page, int size);
    Optional<AssessmentPolicy> findActivePolicy(UUID schoolId, UUID languageId, UUID classId, UUID gradeId, UUID gradeLevelId, Instant atTime);
    boolean existsByFrameworkVersionId(UUID frameworkVersionId);

    // Checks whether any Assessment Policy still applies specifically to a class (used when deleting a class)
    boolean existsBySchoolClassId(UUID schoolClassId);

    // A scope may have exactly one policy in effect, regardless of which Rubric version it points at
    boolean existsActiveForScopeAnyRubricVersion(UUID schoolId, UUID languageId, UUID frameworkVersionId,
            UUID gradeLevelId, UUID schoolGradeId, UUID schoolClassId);

    // Fetches ALL Assessment Policies (every status, including ARCHIVED) within the schoolId scope (null = system-wide).
    // Used to prefetch once and then compute in memory (the active scope to block duplicates, the largest version per
    // scope to issue the next version) during bulk import, instead of calling existsActiveForScope/findMaxVersionForScope
    // separately for every row/scope in the Excel file (avoids N+1 queries).
    List<AssessmentPolicy> findAllForOwner(UUID schoolId);

    // Gets the largest version that ever existed for the scope (including ARCHIVED) to compute the next version
    int findMaxVersionForScope(UUID schoolId, UUID languageId, UUID frameworkVersionId,
            UUID gradeLevelId, UUID schoolGradeId, UUID schoolClassId);

    // Checks whether any Assessment Policy linked to this Rubric Version is already in status PUBLISHED
    boolean existsPublishedByRubricVersionId(UUID rubricVersionId);

    // Checks whether any Assessment Policy linked to this Rubric Version is NOT yet in status PUBLISHED
    boolean existsNotPublishedByRubricVersionId(UUID rubricVersionId);

    // Whether any OTHER Assessment Policy (excluding excludedPolicyId) still uses this Rubric Version and is still
    // in effect (DRAFT or PUBLISHED). Used when archiving a Policy to know whether the Rubric Version may be archived
    // along with it -- since V44 several Policies share one version, so an unconditional archive
    // would pull the scoring scale out from under the remaining Policies.
    boolean existsOtherActiveByRubricVersionId(UUID rubricVersionId, UUID excludedPolicyId);

    // Whether any Assessment Policy still uses this Rubric Version and is still in effect (DRAFT or PUBLISHED)
    // -- excluding no policy (unlike existsOtherActiveByRubricVersionId above, which
    // is used while handling ONE specific policy). Used when archiving a Rubric Version DIRECTLY (not
    // through archiving a Policy) to block it: archiving straight away a version still referenced by a PUBLISHED/DRAFT Policy
    // would leave a Policy pointing at an ARCHIVED version, exactly the state that
    // existsOtherActiveByRubricVersionId was created to prevent in the other direction.
    boolean existsActiveByRubricVersionId(UUID rubricVersionId);

    // List of system Assessment Policies (schoolId IS NULL) in DRAFT linked to this Rubric Version (used for bulk publish)
    List<AssessmentPolicy> findDraftSystemWideByRubricVersionId(UUID rubricVersionId);

    // The published TEMPLATE policy attached to a system Rubric version -- what a school sees and
    // picks when copying that rubric set over (CloneSystemRubricToSchoolUseCase).
    List<AssessmentPolicy> findPublishedSystemWideByRubricVersionId(UUID rubricVersionId);

    // List of Assessment Policies of a school in DRAFT linked to this Rubric Version (used for bulk publish)
    List<AssessmentPolicy> findDraftBySchoolIdAndRubricVersionId(UUID schoolId, UUID rubricVersionId);

    void deleteById(UUID id);

    record CurrentPolicy(UUID rubricVersionId, UUID targetFrameworkBandId) {
    }

    // The scoring policy in effect for a student (used when a practice session starts)
    List<CurrentPolicy> findCurrentPolicyForStudent(UUID studentId);
}
