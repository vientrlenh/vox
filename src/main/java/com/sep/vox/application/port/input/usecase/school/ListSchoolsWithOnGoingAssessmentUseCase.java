package com.sep.vox.application.port.input.usecase.school;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.repository.SchoolRepository;

@Service
public class ListSchoolsWithOnGoingAssessmentUseCase implements IUseCase<Void, List<UUID>> {

    private final SchoolRepository schoolRepository;

    public ListSchoolsWithOnGoingAssessmentUseCase(SchoolRepository schoolRepository) {
        this.schoolRepository = schoolRepository;
    }

    @Override
    public List<UUID> execute(Void input) {
        var result = schoolRepository.findIdsWithOngoingAssessment(Instant.now());
        if (result == null) {
            return List.of();
        }
        return result;
    }

}