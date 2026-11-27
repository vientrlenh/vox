package com.sep.vox.application.port.input.service;

import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.domain.model.assessment.AssessmentPaper;
import com.sep.vox.domain.repository.AssessmentPaperRepository;

/**
 * Picks the paper to operate on in a class test.
 *
 * <p>Since teachers can author several papers for one class test, "take the first paper" is no longer a safe
 * guess: it would edit a paper the user does not know about. So leaving {@code paperId} empty
 * is valid only when the test has exactly one paper.
 */
@Service
@RequiredArgsConstructor
public class ClassTestPaperResolver {

    private final AssessmentPaperRepository assessmentPaperRepository;

    public AssessmentPaper resolve(UUID assessmentId, UUID requestedPaperId) {
        List<AssessmentPaper> papers = assessmentPaperRepository.findByAssessmentId(assessmentId);
        if (papers.isEmpty()) {
            throw new ResourceNotFoundException("The assessment has no paper yet");
        }
        if (requestedPaperId != null) {
            return papers.stream()
                .filter(paper -> paper.getId().equals(requestedPaperId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Paper not found in this assessment"));
        }
        if (papers.size() > 1) {
            throw new IllegalArgumentException("The assessment has several papers; the paper to operate on must be specified");
        }
        return papers.get(0);
    }
}
