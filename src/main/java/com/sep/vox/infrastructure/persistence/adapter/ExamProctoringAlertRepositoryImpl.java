package com.sep.vox.infrastructure.persistence.adapter;

import java.util.List;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.exam.ExamProctoringAlert;
import com.sep.vox.domain.repository.ExamProctoringAlertRepository;
import com.sep.vox.infrastructure.persistence.mapper.ExamProctoringAlertMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataExamProctoringAlertRepository;

@Repository
public class ExamProctoringAlertRepositoryImpl implements ExamProctoringAlertRepository {

    private final SpringDataExamProctoringAlertRepository springDataExamProctoringAlertRepository;

    public ExamProctoringAlertRepositoryImpl(
            SpringDataExamProctoringAlertRepository springDataExamProctoringAlertRepository) {
        this.springDataExamProctoringAlertRepository = springDataExamProctoringAlertRepository;
    }

    /**
     * Chống trùng hai lớp, và lớp thứ hai mới là lớp thật.
     *
     * <p>Kiểm tra tồn tại trước chỉ để tránh ném exception trong trường hợp thường gặp (Kafka gửi lại
     * một message đã xử lý xong từ lâu). Nhưng kiểm-rồi-ghi vốn có khe hở: hai instance consumer cùng
     * nhận một message trong lúc rebalance sẽ cùng vượt qua bước kiểm tra. Ràng buộc unique trên
     * {@code event_id} mới là thứ đóng khe hở đó, nên vi phạm ràng buộc ở đây KHÔNG phải lỗi -- nó
     * chính là cơ chế chống trùng đang làm việc, và phải được nuốt thay vì đẩy message vào DLT.
     *
     * <p>Dùng {@code saveAndFlush} để va chạm nổ ra ngay trong khối try này, thay vì lúc commit ở
     * một tầng khác nơi không ai còn biết nó nghĩa là gì.
     */
    @Override
    public boolean saveIfAbsent(ExamProctoringAlert alert) {
        if (springDataExamProctoringAlertRepository.existsByEventId(alert.getEventId())) {
            return false;
        }
        try {
            springDataExamProctoringAlertRepository.saveAndFlush(ExamProctoringAlertMapper.toJpa(alert));
            return true;
        } catch (DataIntegrityViolationException ex) {
            return false;
        }
    }

    @Override
    public List<ExamProctoringAlert> findByExamSessionIdOrderByCapturedAt(UUID examSessionId) {
        return springDataExamProctoringAlertRepository.findByExamSessionIdOrderByCapturedAtAsc(examSessionId)
            .stream()
            .map(ExamProctoringAlertMapper::toDomain)
            .toList();
    }

    @Override
    public List<ExamProctoringAlert> findByScheduleIdOrderByCapturedAt(UUID scheduleId) {
        return springDataExamProctoringAlertRepository.findByScheduleIdOrderByCapturedAtAsc(scheduleId)
            .stream()
            .map(ExamProctoringAlertMapper::toDomain)
            .toList();
    }
}
