package com.sep.vox.application.port.input.usecase.practicesession;

import com.sep.vox.application.query.dto.PracticeDashboardCountsInfo;
import java.time.LocalDate;

import java.util.UUID;
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
        UUID studentId = userContextPort.getCurrentAuthenticatedUserId();
        PracticeDashboardCountsInfo counts = practiceSessionQueryRepository.findDashboardCounts(studentId);
        int streak = computeStreak(
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
        LocalDate today = LocalDate.now();
        LocalDate expected = completedDatesDesc.get(0).equals(today) ? today : today.minusDays(1);
        int streak = 0;
        for (LocalDate date : completedDatesDesc) {
            if (!date.equals(expected)) {
                break;
            }
            streak++;
            expected = expected.minusDays(1);
        }
        return streak;
    }
}
