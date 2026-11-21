package com.sep.vox.application.usecase.recording;

import static org.assertj.core.api.Assertions.assertThat;
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

import com.sep.vox.application.port.input.command.RecordRecordingPartChangedCommand;
import com.sep.vox.application.port.input.usecase.recording.RecordRecordingPartChangedUseCase;
import com.sep.vox.application.port.output.RecordingStorageConfigPort;
import com.sep.vox.domain.model.assessment.AssessmentRecording;
import com.sep.vox.domain.model.assessment.AssessmentRecordingAssemblyStatus;
import com.sep.vox.domain.model.assessment.AssessmentRequiredStreamType;
import com.sep.vox.domain.model.assessment.AssessmentSession;
import com.sep.vox.domain.repository.AssessmentRecordingRepository;
import com.sep.vox.domain.repository.AssessmentSessionRepository;

@ExtendWith(MockitoExtension.class)
class RecordRecordingPartChangedUseCaseTests {

    private static final String SOURCE = "SERVER_WEBRTC";

    @Mock
    private AssessmentSessionRepository assessmentSessionRepository;
    @Mock
    private AssessmentRecordingRepository assessmentRecordingRepository;
    @Mock
    private RecordingStorageConfigPort recordingStorageConfigPort;
    @InjectMocks
    private RecordRecordingPartChangedUseCase useCase;

    private final UUID sessionId = UUID.randomUUID();

    @Test
    void should_store_configured_bucket_and_key_when_event_has_object_key() {
        givenNoExistingRecording();
        when(recordingStorageConfigPort.bucket()).thenReturn("vox-recordings");

        useCase.execute(commandWithObjectKey("sessions/a/part-1.webm"));

        AssessmentRecording saved = savedRecording();
        assertThat(saved.getBucket()).isEqualTo("vox-recordings");
        assertThat(saved.getS3Key()).isEqualTo("sessions/a/part-1.webm");
    }

    @Test
    void should_leave_bucket_and_key_empty_when_event_has_no_object_key() {
        givenNoExistingRecording();

        useCase.execute(commandWithObjectKey(null));

        AssessmentRecording saved = savedRecording();
        assertThat(saved.getBucket()).isEmpty();
        assertThat(saved.getS3Key()).isEmpty();
    }

    private void givenNoExistingRecording() {
        when(assessmentSessionRepository.findById(sessionId))
            .thenReturn(Optional.of(AssessmentSession.builder().build()));
        when(assessmentRecordingRepository.findByAssessmentSessionIdAndStreamTypeAndSource(
            sessionId, AssessmentRequiredStreamType.CAMERA, SOURCE)).thenReturn(Optional.empty());
    }

    private AssessmentRecording savedRecording() {
        ArgumentCaptor<AssessmentRecording> captor = ArgumentCaptor.forClass(AssessmentRecording.class);
        verify(assessmentRecordingRepository).save(captor.capture());
        return captor.getValue();
    }

    private RecordRecordingPartChangedCommand commandWithObjectKey(String objectKey) {
        return new RecordRecordingPartChangedCommand(
            sessionId, UUID.randomUUID(), AssessmentRequiredStreamType.CAMERA,
            AssessmentRecordingAssemblyStatus.PROCESSING, objectKey, null, Instant.now(), SOURCE);
    }
}
