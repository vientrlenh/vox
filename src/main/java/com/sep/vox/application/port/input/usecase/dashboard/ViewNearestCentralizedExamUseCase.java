package com.sep.vox.application.port.input.usecase.dashboard;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.dto.NearestCentralizedExamDto;
import com.sep.vox.application.query.repository.NearestCentralizedExamQueryRepository;

/** Kỳ thi tập trung gần thời điểm hiện tại nhất của trường — dùng cho dashboard school admin. */
@Service
public class ViewNearestCentralizedExamUseCase implements IUseCase<Void, Optional<NearestCentralizedExamDto>> {

    private final NearestCentralizedExamQueryRepository nearestCentralizedExamQueryRepository;
    private final UserContextPort userContextPort;

    public ViewNearestCentralizedExamUseCase(
            NearestCentralizedExamQueryRepository nearestCentralizedExamQueryRepository,
            UserContextPort userContextPort) {
        this.nearestCentralizedExamQueryRepository = nearestCentralizedExamQueryRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<NearestCentralizedExamDto> execute(Void input) {
        var schoolId = userContextPort.getCurrentSchoolId();
        return nearestCentralizedExamQueryRepository.findNearestForSchool(schoolId);
    }
}
