package com.sep.vox.application.service;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sep.vox.application.port.input.service.PracticeTopicOfferEnrichmentService;
import com.sep.vox.application.port.output.PracticeFrameworkConfigPort;
import com.sep.vox.domain.repository.FrameworkResultBandRepository;
import com.sep.vox.domain.repository.FrameworkVersionRepository;
import com.sep.vox.domain.repository.LearnerProfileRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;

@ExtendWith(MockitoExtension.class)
class PracticeTopicOfferEnrichmentServiceTests {

    @Mock
    private LearnerProfileRepository learnerProfileRepository;
    @Mock
    private SchoolSubscriptionRepository schoolSubscriptionRepository;
    @Mock
    private FrameworkResultBandRepository frameworkResultBandRepository;
    @Mock
    private FrameworkVersionRepository frameworkVersionRepository;
    @Mock
    private PracticeFrameworkConfigPort practiceFrameworkConfigPort;

    @Test
    void should_look_up_active_framework_without_code_when_configured_code_is_blank() {
        verifyCodeReachingLookup("   ", null);
    }

    @Test
    void should_look_up_active_framework_with_trimmed_code_when_configured_code_is_padded() {
        verifyCodeReachingLookup("  VSTEP  ", "VSTEP");
    }

    private void verifyCodeReachingLookup(String configuredCode, String expectedLookupCode) {
        when(practiceFrameworkConfigPort.frameworkVersionCode()).thenReturn(configuredCode);
        when(frameworkVersionRepository.findActiveVersionId(expectedLookupCode)).thenReturn(Optional.empty());
        when(learnerProfileRepository.findFrameworkBandCount(org.mockito.ArgumentMatchers.any(UUID.class)))
            .thenReturn(List.of());
        PracticeTopicOfferEnrichmentService service = new PracticeTopicOfferEnrichmentService(
            learnerProfileRepository, schoolSubscriptionRepository, frameworkResultBandRepository,
            frameworkVersionRepository, practiceFrameworkConfigPort);

        service.frameworkBandCount();

        verify(frameworkVersionRepository).findActiveVersionId(expectedLookupCode);
    }
}
