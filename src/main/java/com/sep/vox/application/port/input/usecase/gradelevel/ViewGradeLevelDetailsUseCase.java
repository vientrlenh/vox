package com.sep.vox.application.port.input.usecase.gradelevel;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.query.ViewGradeLevelDetailsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.GradeLevelDto;
import com.sep.vox.domain.model.gradelevel.GradeLevel;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.GradeLevelRepository;
import com.sep.vox.domain.repository.UserRepository;

@Service
public class ViewGradeLevelDetailsUseCase implements IUseCase<ViewGradeLevelDetailsQuery, GradeLevelDto> {

    private final GradeLevelRepository gradeLevelRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;

    public ViewGradeLevelDetailsUseCase(
            GradeLevelRepository gradeLevelRepository,
            UserRepository userRepository,
            UserContextPort userContextPort) {
        this.gradeLevelRepository = gradeLevelRepository;
        this.userRepository = userRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional(readOnly = true)
    public GradeLevelDto execute(ViewGradeLevelDetailsQuery input) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        if (!userRepository.existsByIdAndStatus(currentUserId, UserStatus.ACTIVE)) {
            throw new UnauthorizedException("Tài khoản không tồn tại hoặc đã bị khóa.");
        }

        GradeLevel gradeLevel = gradeLevelRepository.findById(input.gradeLevelId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy khối học."));

        return GradeLevelDto.toDto(gradeLevel);
    }
}
