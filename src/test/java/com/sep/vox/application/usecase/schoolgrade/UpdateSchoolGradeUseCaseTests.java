package com.sep.vox.application.usecase.schoolgrade;

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

import com.sep.vox.application.port.input.command.schoolgrade.UpdateSchoolGradeCommand;
import com.sep.vox.application.port.input.usecase.schoolgrade.UpdateSchoolGradeUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.school.SchoolGrade;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.SchoolGradeRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UpdateSchoolGradeUseCaseTests {

    private final UUID gradeId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @Mock
    private SchoolGradeRepository schoolGradeRepository;
    @Mock
    private SchoolUserRepository schoolUserRepository;
    @Mock
    private UserContextPort userContextPort;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UpdateSchoolGradeUseCase useCase;

    @BeforeEach
    void setUp() {
        when(schoolGradeRepository.findById(gradeId)).thenReturn(Optional.of(SchoolGrade.builder().id(gradeId).build()));
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        when(userRepository.existsByIdAndStatus(userId, UserStatus.ACTIVE)).thenReturn(true);
        when(userContextPort.isSystemAdmin()).thenReturn(true);
        when(schoolGradeRepository.updateSchoolGradeAtomic(any(), any(), any(), anyBoolean(), any(), any())).thenReturn(1);
    }

    @Test
    void execute_clearsTheDescriptionWhenProvidedAsNull() {
        useCase.execute(new UpdateSchoolGradeCommand(gradeId, null, null, true, null, null));

        verify(schoolGradeRepository).updateSchoolGradeAtomic(eq(gradeId), isNull(), isNull(), eq(true), isNull(),
            isNull());
    }

    @Test
    void execute_keepsTheDescriptionWhenNotProvided() {
        useCase.execute(new UpdateSchoolGradeCommand(gradeId, "Grade", null, false, null, null));

        verify(schoolGradeRepository).updateSchoolGradeAtomic(eq(gradeId), eq("Grade"), isNull(), eq(false), isNull(),
            isNull());
    }
}
