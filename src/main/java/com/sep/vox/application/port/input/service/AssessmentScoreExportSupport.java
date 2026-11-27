package com.sep.vox.application.port.input.service;

import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.query.assessmentgrading.ExportAssessmentScoresQuery;
import com.sep.vox.application.query.dto.AssessmentScoreRowInfo;
import com.sep.vox.application.query.dto.GradingAssignmentFilter;
import com.sep.vox.application.query.repository.AssessmentGradingQueryRepository;
import com.sep.vox.domain.service.assessment.GradingScopeKind;

/**
 * The shared part of every score-export format: authorization, fixing the scope, loading rows.
 *
 * <p>There are two export use cases (CSV and Excel) and they only differ in how the file is written. If each
 * called authorization on its own, sooner or later one would be tightened while the other is forgotten — and the
 * forgotten one is a back door to get a school's whole score table.
 */
@Service
@RequiredArgsConstructor
public class AssessmentScoreExportSupport {

    private final AssessmentGradingQueryRepository assessmentGradingQueryRepository;
    private final AssessmentGradingAccessService assessmentGradingAccessService;

    @Transactional(readOnly = true)
    public List<AssessmentScoreRowInfo> loadRows(ExportAssessmentScoresQuery input) {
        UUID currentUserId = assessmentGradingAccessService.requireActiveUserId();
        UUID schoolId = assessmentGradingAccessService.requireCurrentSchoolId(currentUserId);
        // The teacher who created a class test can export the score sheet of EXACTLY their own test --
        // the scope is closed by assessmentId, so no data of another assessment of the school leaks.
        assessmentGradingAccessService.authorizeSchoolAdminOrClassTestChair(
            schoolId, input.assessmentId(), currentUserId);

        // No scope = export every assessment of the whole school: tens of thousands of rows built
        // in RAM, with three unpaged queries using a huge IN clause. Require choosing a
        // scope, same mould as AutoAssignGradingUseCase.
        if (input.assessmentId() == null && input.scheduleId() == null) {
            throw new IllegalArgumentException("An assessment or schedule must be chosen to export the score table.");
        }

        return assessmentGradingQueryRepository.findScoreRows(toFilter(input, schoolId));
    }

    /**
     * {@code schoolId} comes from the login session, not from the query — this is the only guard that
     * keeps the exported file inside the caller's school.
     */
    private GradingAssignmentFilter toFilter(ExportAssessmentScoresQuery input, UUID schoolId) {
        return new GradingAssignmentFilter(
            schoolId,
            input.assessmentId(),
            input.scheduleId(),
            input.teacherId(),
            nameOrNull(input.resultStatus()),
            nameOrNull(input.roundType()),
            nameOrNull(input.assignmentStatus()),
            input.unassignedOnly(),
            input.overdueOnly(),
            input.hasOpenAppeal(),
            blankToNull(input.keyword()),
            GradingScopeKind.orCentralized(nameOrNull(input.assessmentKind())));
    }

    /**
     * An empty string from the query string means "no filter", not "filter by an empty value".
     * Without normalizing it to null, {@code cr.status = ''} matches exactly 0 rows and the export file comes out empty.
     */
    private String nameOrNull(Enum<?> value) {
        return value == null ? null : value.name();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
