package com.sep.vox.interfaces.graphql.controller;

import java.util.UUID;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import com.sep.vox.application.port.input.query.ViewImportSessionQuery;
import com.sep.vox.application.port.input.query.ViewImportSessionsQuery;
import com.sep.vox.application.port.input.usecase.importfile.ViewImportSessionUseCase;
import com.sep.vox.application.port.input.usecase.importfile.ViewImportSessionsUseCase;
import com.sep.vox.application.response.input.importfile.ImportSessionDetailsResponse;
import com.sep.vox.application.response.input.importfile.ImportSessionSummaryResponse;
import com.sep.vox.domain.common.PageResult;

@Controller("graphqlImportController")
public class ImportController {

    private final ViewImportSessionUseCase viewImportSessionUseCase;
    private final ViewImportSessionsUseCase viewImportSessionsUseCase;

    public ImportController(
            ViewImportSessionUseCase viewImportSessionUseCase,
            ViewImportSessionsUseCase viewImportSessionsUseCase) {
        this.viewImportSessionUseCase = viewImportSessionUseCase;
        this.viewImportSessionsUseCase = viewImportSessionsUseCase;
    }

    @QueryMapping(name = "importSession")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ImportSessionDetailsResponse importSession(@Argument(name = "id") UUID id) {
        return viewImportSessionUseCase.execute(new ViewImportSessionQuery(id));
    }

    @QueryMapping(name = "importSessions")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public PageResult<ImportSessionSummaryResponse> importSessions(
            @Argument(name = "page") int page,
            @Argument(name = "size") int size,
            @Argument(name = "type") String type,
            @Argument(name = "status") String status) {
        return viewImportSessionsUseCase.execute(new ViewImportSessionsQuery(page, size, type, status));
    }
}
