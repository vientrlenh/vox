package com.sep.vox.application.port.input.usecase.schooldirectory;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.DuplicatedException;
import com.sep.vox.application.port.input.command.CreateSchoolDirectoryCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.schooldirectory.CreateSchoolDirectoryResponse;
import com.sep.vox.domain.model.school.SchoolDirectory;
import com.sep.vox.domain.repository.SchoolDirectoryRepository;

@Service
public class CreateSchoolDirectoryUseCase implements IUseCase<CreateSchoolDirectoryCommand, CreateSchoolDirectoryResponse> {

    private final SchoolDirectoryRepository schoolDirectoryRepository;
    private final UserContextPort userContextPort;

    public CreateSchoolDirectoryUseCase(SchoolDirectoryRepository schoolDirectoryRepository, UserContextPort userContextPort) {
        this.schoolDirectoryRepository = schoolDirectoryRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public CreateSchoolDirectoryResponse execute(CreateSchoolDirectoryCommand input) {
        var command = normalize(input);

        if (schoolDirectoryRepository.existsByCode(command.code())) {
            throw new DuplicatedException("Mã trường này đã tồn tại trong danh mục hệ thống");
        }

        var now = Instant.now();
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();

        var directory = SchoolDirectory.createByAdmin(
            command.code(), 
            command.name(), 
            command.provinceCode(), 
            command.provinceName(), 
            command.districtName(), 
            command.domain(), 
            command.address(), 
            now, 
            currentUserId
        );
        var saved = schoolDirectoryRepository.save(directory);
        return new CreateSchoolDirectoryResponse(saved.getId());
    }
    

    private CreateSchoolDirectoryCommand normalize(CreateSchoolDirectoryCommand input) {
        return new CreateSchoolDirectoryCommand(
            StringNormalization.normalizeCode(input.code()), 
            StringNormalization.trimAndCollapseSpaces(input.name()), 
            StringNormalization.normalizeCode(input.provinceCode()), 
            StringNormalization.trimAndCollapseSpaces(input.provinceName()), 
            StringNormalization.trimAndCollapseSpaces(input.districtName()), 
            StringNormalization.normalizeDomain(input.domain()), 
            StringNormalization.trimAndCollapseSpaces(input.address())
        );
    }
}
