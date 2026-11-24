package com.sep.vox.interfaces.graphql.dataloader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.UUID;

import graphql.GraphQLContext;
import org.dataloader.DataLoader;
import org.dataloader.DataLoaderRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.graphql.execution.DefaultBatchLoaderRegistry;

import com.sep.vox.application.port.input.usecase.assessmentevaluation.ResolveAssessmentCandidateAttemptsUseCase;
import com.sep.vox.domain.model.assessment.AssessmentCandidate;
import com.sep.vox.domain.model.school.SchoolClassUser;
import com.sep.vox.domain.repository.AssessmentBlueprintRepository;
import com.sep.vox.domain.repository.AssessmentBlueprintSlotRepository;
import com.sep.vox.domain.repository.AssessmentBlueprintVersionRepository;
import com.sep.vox.domain.repository.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.AssessmentMemberRepository;
import com.sep.vox.domain.repository.AssessmentPaperItemRepository;
import com.sep.vox.domain.repository.AssessmentPaperRepository;
import com.sep.vox.domain.repository.AssessmentPaperSectionRepository;
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.repository.AssessmentScheduleRepository;
import com.sep.vox.domain.repository.AssessmentSecurePoolRepository;
import com.sep.vox.domain.repository.SchoolClassUserRepository;

class AssessmentSchoolClassIdLoaderTests {

    private final AssessmentCandidateRepository assessmentCandidateRepository = mock(AssessmentCandidateRepository.class);
    private final SchoolClassUserRepository schoolClassUserRepository = mock(SchoolClassUserRepository.class);

    @Test
    void should_resolve_the_active_class_of_the_first_candidate_with_one_candidate_row_per_assessment() {
        UUID assessmentId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();
        when(assessmentCandidateRepository.findFirstCandidateByAssessmentIdIn(anyCollection()))
            .thenReturn(List.of(candidate(assessmentId, studentId)));
        when(schoolClassUserRepository.findByUserIdIn(anyCollection()))
            .thenReturn(List.of(membership(studentId, classId)));

        DataLoader<UUID, UUID> loader = loader();
        CompletableFuture<UUID> pending = loader.load(assessmentId);
        loader.dispatch();
        UUID resolvedClassId = pending.join();

        assertThat(resolvedClassId).isEqualTo(classId);
        verify(assessmentCandidateRepository, never()).findByAssessmentIdIn(anyCollection());
    }

    private DataLoader<UUID, UUID> loader() {
        DefaultBatchLoaderRegistry registry = new DefaultBatchLoaderRegistry();
        new AssessmentGraphQLDataLoaderConfig(registry, mock(AssessmentRepository.class),
            mock(AssessmentScheduleRepository.class), mock(AssessmentPaperRepository.class),
            mock(AssessmentBlueprintRepository.class), mock(AssessmentBlueprintVersionRepository.class),
            mock(AssessmentBlueprintSlotRepository.class), mock(AssessmentMemberRepository.class),
            mock(AssessmentPaperSectionRepository.class), mock(AssessmentPaperItemRepository.class),
            mock(AssessmentSecurePoolRepository.class), assessmentCandidateRepository, schoolClassUserRepository,
            mock(ResolveAssessmentCandidateAttemptsUseCase.class)).registerLoaders();
        DataLoaderRegistry dataLoaderRegistry = new DataLoaderRegistry();
        registry.registerDataLoaders(dataLoaderRegistry, GraphQLContext.newContext().build());
        DataLoader<UUID, UUID> loader = dataLoaderRegistry.getDataLoader(
            AssessmentGraphQLDataLoaderConfig.ASSESSMENT_SCHOOL_CLASS_ID_BY_ASSESSMENT_ID);
        return loader;
    }

    private AssessmentCandidate candidate(UUID assessmentId, UUID studentId) {
        return AssessmentCandidate.builder().assessmentId(assessmentId).studentId(studentId).build();
    }

    private SchoolClassUser membership(UUID studentId, UUID classId) {
        return SchoolClassUser.builder().userId(studentId).schoolClassId(classId).active(true).build();
    }
}
