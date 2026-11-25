package com.sep.vox.application.usecase.practicesession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sep.vox.application.port.input.service.StartPracticeSessionPersistenceService;
import com.sep.vox.application.query.repository.PracticeSessionQueryRepository;
import com.sep.vox.domain.model.personalization.PracticePaper;
import com.sep.vox.domain.model.personalization.PracticeSession;
import com.sep.vox.domain.repository.PracticePaperRepository;
import com.sep.vox.domain.repository.PracticeSessionRepository;

@ExtendWith(MockitoExtension.class)
class StartPracticeSessionPersistenceServiceTests {

    @Mock
    private PracticeSessionRepository practiceSessionRepository;

    @Mock
    private PracticePaperRepository practicePaperRepository;

    @Mock
    private PracticeSessionQueryRepository practiceSessionQueryRepository;

    @InjectMocks
    private StartPracticeSessionPersistenceService service;

    @Test
    void should_read_the_session_row_back_with_the_id_returned_by_save_and_not_mint_one() {
        UUID studentId = UUID.randomUUID();
        UUID databaseGeneratedSessionId = UUID.randomUUID();
        PracticePaper paper = reservedPaper(studentId);
        when(practicePaperRepository.findReservedPaper(any(), any(), any())).thenReturn(Optional.of(paper));
        when(practiceSessionRepository.save(any(PracticeSession.class)))
            .thenReturn(PracticeSession.builder().id(databaseGeneratedSessionId).build());
        when(practiceSessionQueryRepository.findSessionRow(databaseGeneratedSessionId, studentId))
            .thenReturn(Optional.empty());

        service.persist(studentId, paper.getId());

        ArgumentCaptor<PracticeSession> savedSession = ArgumentCaptor.forClass(PracticeSession.class);
        verify(practiceSessionRepository).save(savedSession.capture());
        assertThat(savedSession.getValue().getId()).isNull();
        verify(practiceSessionQueryRepository).findSessionRow(databaseGeneratedSessionId, studentId);
    }

    private PracticePaper reservedPaper(UUID studentId) {
        return PracticePaper.builder()
            .id(UUID.randomUUID())
            .studentId(studentId)
            .practiceTopicId(UUID.randomUUID())
            .targetFrameworkBandId(UUID.randomUUID())
            .origin("SELECTED")
            .status("RESERVED")
            .expiresAt(Instant.parse("2099-01-01T00:00:00Z"))
            .build();
    }
}
