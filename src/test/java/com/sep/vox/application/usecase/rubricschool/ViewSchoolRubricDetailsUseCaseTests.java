package com.sep.vox.application.usecase.rubricschool;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.query.ViewSchoolRubricDetailsQuery;
import com.sep.vox.application.port.input.usecase.rubricschool.ViewSchoolRubricDetailsUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.rubric.Rubric;
import com.sep.vox.domain.model.rubric.RubricOwnerType;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.RubricRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class ViewSchoolRubricDetailsUseCaseTests {

    private static final UUID CURRENT_USER_ID = UUID.randomUUID();
    private static final UUID RUBRIC_ID = UUID.randomUUID();
    private static final UUID CALLER_SCHOOL_ID = UUID.randomUUID();
    private static final UUID OTHER_SCHOOL_ID = UUID.randomUUID();

    @Mock
    private RubricRepository rubricRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserContextPort userContextPort;
    @Mock
    private SchoolRepository schoolRepository;
    @Mock
    private SchoolUserRepository schoolUserRepository;

    @InjectMocks
    private ViewSchoolRubricDetailsUseCase useCase;

    @Test
    void should_throw_forbidden_when_the_rubric_belongs_to_another_school() {
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(CURRENT_USER_ID);
        when(userRepository.findById(CURRENT_USER_ID))
            .thenReturn(Optional.of(User.builder().status(UserStatus.ACTIVE).build()));
        when(rubricRepository.findById(RUBRIC_ID)).thenReturn(Optional.of(
            Rubric.builder().id(RUBRIC_ID).ownerType(RubricOwnerType.SCHOOL).schoolId(OTHER_SCHOOL_ID).build()));
        when(schoolUserRepository.findByUserId(CURRENT_USER_ID)).thenReturn(Optional.of(
            SchoolUser.builder().userId(CURRENT_USER_ID).schoolId(CALLER_SCHOOL_ID).build()));

        assertThatThrownBy(() -> useCase.execute(new ViewSchoolRubricDetailsQuery(OTHER_SCHOOL_ID, RUBRIC_ID)))
            .isInstanceOf(ForbiddenException.class)
            .hasMessage("BẢO MẬT: Bạn không có quyền xem Rubric của trường khác.");
    }
}
