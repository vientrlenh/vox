package com.sep.vox.interfaces.graphql.controller;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import com.sep.vox.application.port.input.command.interestdimension.UpsertInterestDimensionCommand;
import com.sep.vox.application.port.input.usecase.interestdimension.ManageInterestDimensionUseCase;
import com.sep.vox.application.response.input.interestdimension.InterestDimensionResponses.InterestDimensionResponse;
import com.sep.vox.interfaces.graphql.dto.request.interestdimension.UpsertInterestDimensionInput;

@Controller
@RequiredArgsConstructor
public class GraphQLInterestDimensionController {

    private final ManageInterestDimensionUseCase manageInterestDimensionUseCase;

    @QueryMapping
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public List<InterestDimensionResponse> interestDimensions() {
        return manageInterestDimensionUseCase.findAll();
    }

    @MutationMapping
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public InterestDimensionResponse createInterestDimension(
            @Argument("input") UpsertInterestDimensionInput input) {
        return manageInterestDimensionUseCase.create(toCommand(input));
    }

    @MutationMapping
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public InterestDimensionResponse updateInterestDimension(
            @Argument("input") UpsertInterestDimensionInput input) {
        return manageInterestDimensionUseCase.update(toCommand(input));
    }

    @MutationMapping
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public boolean deactivateInterestDimension(@Argument("code") String code) {
        return manageInterestDimensionUseCase.deactivate(code);
    }

    private static UpsertInterestDimensionCommand toCommand(UpsertInterestDimensionInput input) {
        return new UpsertInterestDimensionCommand(
            input.code(),
            input.label(),
            input.description(),
            input.active(),
            input.quizEligible(),
            input.displayOrder()
        );
    }
}
