package com.sep.vox.application.port.input.usecase.proctoring;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.service.ExamRecordingAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.dto.ExamProctoringAlertDto;
import com.sep.vox.domain.mapper.ExamProctoringAlertDtoMapper;
import com.sep.vox.domain.repository.ExamProctoringAlertRepository;

/**
 * Cảnh báo giám sát của một phiên thi, theo thứ tự thời gian.
 *
 * <p>Dùng chung {@link ExamRecordingAccessService} với đường xem bản ghi, không dựng luật quyền
 * riêng. Hai thứ này là cùng một loại bằng chứng về cùng một con người, nên ai xem được video thì
 * xem được cảnh báo, và ngược lại -- hai luật riêng cho cùng một câu hỏi thì sớm muộn cũng lệch, và
 * lệch về phía lỏng hơn là rò rỉ dữ liệu hành vi của học viên.
 *
 * <p><b>Chưa làm:</b> quy {@code capturedAt} về mốc tua trong video. Việc đó cần thời điểm bắt đầu
 * của bản ghi, mà {@code ExamRecording} hiện không lưu: {@code createdAt} là lúc tạo dòng dữ liệu và
 * {@code assembledAt} là lúc ghép xong, không cái nào là mốc bắt đầu của media. Nguồn có sẵn ở
 * vox-streaming (suy ra được từ {@code StreamEndedEvent.EndedAt - Duration} cho đường WebRTC, và
 * {@code UploadSession.CreatedAt} cho đường desktop), nhưng đưa nó về đây phải sửa hợp đồng của
 * {@code RecordingPartChangedEvent} -- tức là động vào đường lắp ráp bản ghi, một thay đổi có bán
 * kính ảnh hưởng khác hẳn và xứng đáng đi riêng.
 */
@Service
public class ViewExamSessionProctoringAlertsUseCase implements IUseCase<UUID, List<ExamProctoringAlertDto>> {

    private final ExamRecordingAccessService accessService;
    private final ExamProctoringAlertRepository examProctoringAlertRepository;

    public ViewExamSessionProctoringAlertsUseCase(
            ExamRecordingAccessService accessService,
            ExamProctoringAlertRepository examProctoringAlertRepository) {
        this.accessService = accessService;
        this.examProctoringAlertRepository = examProctoringAlertRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExamProctoringAlertDto> execute(UUID examSessionId) {
        var session = accessService.requireCanViewRecordings(examSessionId);
        return examProctoringAlertRepository.findByExamSessionIdOrderByCapturedAt(session.getId())
            .stream()
            .map(ExamProctoringAlertDtoMapper::toDto)
            .toList();
    }
}
