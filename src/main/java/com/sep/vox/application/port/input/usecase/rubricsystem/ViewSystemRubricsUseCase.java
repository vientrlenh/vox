package com.sep.vox.application.port.input.usecase.rubricsystem;

import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.query.rubricsystem.ViewSystemRubricsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.RubricDto;
import com.sep.vox.domain.mapper.RubricDtoMapper;
import com.sep.vox.domain.model.rubric.Rubric;
import com.sep.vox.domain.model.rubric.RubricOwnerType;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.RubricRepository;
import com.sep.vox.domain.repository.UserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
public class ViewSystemRubricsUseCase implements IUseCase<ViewSystemRubricsQuery, PageResult<RubricDto>> {

    private final RubricRepository rubricRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;

    @Override
    @Transactional(readOnly = true)
    public PageResult<RubricDto> execute(ViewSystemRubricsQuery query) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();


        if (!userRepository.existsByIdAndStatus(currentUserId, UserStatus.ACTIVE)) {
            throw new UnauthorizedException("The account does not exist or is locked.");
        }

        PageResult<Rubric> rubricPage = rubricRepository.findAllByOwnerType(
                RubricOwnerType.SYSTEM,
                query.page(),
                query.size()
        );

        return RubricDtoMapper.toRubricPage(rubricPage);
    }
}