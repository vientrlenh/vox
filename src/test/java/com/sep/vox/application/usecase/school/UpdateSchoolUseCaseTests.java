package com.sep.vox.application.usecase.school;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sep.vox.application.port.input.command.school.UpdateSchoolCommand;
import com.sep.vox.application.port.input.usecase.school.UpdateSchoolUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.school.School;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UpdateSchoolUseCaseTests {

    private final UUID schoolId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @Mock
    private SchoolRepository schoolRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SchoolUserRepository schoolUserRepository;
    @Mock
    private UserContextPort userContextPort;

    @InjectMocks
    private UpdateSchoolUseCase useCase;

    @BeforeEach
    void setUp() {
        School school = School.builder().id(schoolId).build();
        when(schoolRepository.findById(schoolId)).thenReturn(Optional.of(school));
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        when(userRepository.existsByIdAndStatus(userId, UserStatus.ACTIVE)).thenReturn(true);
        when(schoolUserRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(schoolRepository.updateSchoolAtomic(any(), any(), any(), anyBoolean(), any(), any(), any(), anyBoolean(),
            any(), any())).thenReturn(1);
    }

    @Test
    void execute_clearsDescriptionAndDomainWithoutNormalizingWhenProvidedAsNull() {
        useCase.execute(new UpdateSchoolCommand(schoolId, null, null, true, null, null, null, true, null, null));

        verify(schoolRepository).updateSchoolAtomic(eq(schoolId), isNull(), isNull(), eq(true), isNull(), isNull(),
            isNull(), eq(true), isNull(), isNull());
    }

    @Test
    void execute_keepsDescriptionAndDomainWhenNotProvided() {
        useCase.execute(new UpdateSchoolCommand(schoolId, "Name", null, false, null, null, null, false, null, null));

        verify(schoolRepository).updateSchoolAtomic(eq(schoolId), eq("Name"), isNull(), eq(false), isNull(), isNull(),
            isNull(), eq(false), isNull(), isNull());
    }

    @Test
    void execute_normalizesTheValuesWhenProvided() {
        useCase.execute(new UpdateSchoolCommand(schoolId, null, "  Some   text ", true, null, null, " Vox.Edu.VN ", true, null, null));

        verify(schoolRepository).updateSchoolAtomic(eq(schoolId), isNull(), eq("Some text"), eq(true), isNull(), isNull(),
            eq("vox.edu.vn"), eq(true), isNull(), isNull());
    }
}
