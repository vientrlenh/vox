package com.sep.vox.application.usecase.framework;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sep.vox.application.exception.DataConflictException;
import com.sep.vox.application.exception.ResourceDuplicatedException;
import com.sep.vox.application.port.input.command.CreateFrameworkCommand;
import com.sep.vox.application.port.input.usecase.framework.CreateFrameworkUseCase;
import com.sep.vox.domain.model.framework.Framework;
import com.sep.vox.domain.repository.FrameworkRepository;

@ExtendWith(MockitoExtension.class)
class CreateFrameworkUseCaseTests {

    @Mock
    private FrameworkRepository frameworkRepository;

    @InjectMocks
    private CreateFrameworkUseCase useCase;

    @Test
    void execute_throwsDuplicatedWhenTheRepositoryReportsADataConflict() {
        when(frameworkRepository.save(any(Framework.class))).thenThrow(new DataConflictException("unique violation", null));
        CreateFrameworkCommand command = new CreateFrameworkCommand("CEFR", "CEFR framework", "Description", true);

        assertThatThrownBy(() -> useCase.execute(command))
            .isInstanceOf(ResourceDuplicatedException.class)
            .hasMessage("Mã framework đã tồn tại");
    }
}
