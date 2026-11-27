package com.sep.vox.application.port.input.usecase.dashboard;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.query.dashboard.ViewGradingFailureSessionsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.input.usecase.dashboard.ViewGradingFailureOverviewUseCase.GradingFailureWindow;
import com.sep.vox.application.query.dto.GradingFailureSessionDto;
import com.sep.vox.application.query.repository.GradingFailureQueryRepository;
import com.sep.vox.domain.common.PageResult;

/** The sessions in ONE cause group on the failed-grading triage page. */
@Service
@RequiredArgsConstructor
public class ViewGradingFailureSessionsUseCase
        implements IUseCase<ViewGradingFailureSessionsQuery, PageResult<GradingFailureSessionDto>> {

    private final GradingFailureQueryRepository gradingFailureQueryRepository;

    /**
     * Does NOT clamp page/size again here: the boundary already called {@code PageArguments.validate}, and clamping
     * a second time is exactly the habit that class's Javadoc forbids -- it turns a bad request into a 200 OK with
     * data from another page.
     */
    @Override
    @Transactional(readOnly = true)
    public PageResult<GradingFailureSessionDto> execute(ViewGradingFailureSessionsQuery input) {
        ViewGradingFailureOverviewUseCase.GradingFailureWindow window = GradingFailureWindow.resolve(input.dateFrom(), input.dateTo());

        // An empty/reversed range: an empty page, not an error, handled like the other dashboard queries -- a user who
        // picks the wrong range sees "no sessions", not an error screen.
        if (window.isEmpty()) {
            return new PageResult<>(List.of(), input.page(), input.size(), 0, 0);
        }

        return gradingFailureQueryRepository.findSessions(
            window.from(), window.to(), input.signature(), input.page(), input.size());
    }
}
