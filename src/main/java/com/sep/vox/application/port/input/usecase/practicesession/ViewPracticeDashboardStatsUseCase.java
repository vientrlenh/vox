package com.sep.vox.application.port.input.usecase.practicesession;

import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.PracticeSessionQueryRepository;
import com.sep.vox.application.response.input.practicesession.PracticeSessionResponses.PracticeDashboardStats;

@Service
public class ViewPracticeDashboardStatsUseCase implements IUseCase<Void, PracticeDashboardStats> {

    private final PracticeSessionQueryRepository practiceSessionQueryRepository;
    private final UserContextPort userContextPort;

    public ViewPracticeDashboardStatsUseCase(
            PracticeSessionQueryRepository practiceSessionQueryRepository,
            UserContextPort userContextPort) {
        this.practiceSessionQueryRepository = practiceSessionQueryRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional(readOnly = true)
    public PracticeDashboardStats execute(Void input) {
        var studentId = userContextPort.getCurrentAuthenticatedUserId();
        var counts = practiceSessionQueryRepository.findDashboardCounts(studentId);
        var streak = computeStreak(
            practiceSessionQueryRepository.findCompletedSessionDatesDesc(studentId)
        );
        return new PracticeDashboardStats(
            counts.getSessionsDone(),
            counts.getAverageScore(),
            streak
        );
    }

    private int computeStreak(java.util.List<LocalDate> completedDatesDesc) {
        if (completedDatesDesc.isEmpty()) {
            return 0;
        }
        var today = LocalDate.now();
        var expected = completedDatesDesc.get(0).equals(today) ? today : today.minusDays(1);
        var streak = 0;
        for (var date : completedDatesDesc) {
            if (!date.equals(expected)) {
                break;
            }
            streak++;
            expected = expected.minusDays(1);
        }
        return streak;
    }
}
