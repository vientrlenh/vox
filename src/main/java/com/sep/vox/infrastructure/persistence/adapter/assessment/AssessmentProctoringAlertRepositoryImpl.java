package com.sep.vox.infrastructure.persistence.adapter.assessment;

import java.util.List;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.assessment.AssessmentProctoringAlert;
import com.sep.vox.domain.repository.assessment.AssessmentProctoringAlertRepository;
import com.sep.vox.infrastructure.persistence.mapper.assessment.AssessmentProctoringAlertMapper;
import com.sep.vox.infrastructure.persistence.repository.assessment.SpringDataAssessmentProctoringAlertRepository;

@Repository
public class AssessmentProctoringAlertRepositoryImpl implements AssessmentProctoringAlertRepository {

    private final SpringDataAssessmentProctoringAlertRepository springDataAssessmentProctoringAlertRepository;

    public AssessmentProctoringAlertRepositoryImpl(
            SpringDataAssessmentProctoringAlertRepository springDataAssessmentProctoringAlertRepository) {
        this.springDataAssessmentProctoringAlertRepository = springDataAssessmentProctoringAlertRepository;
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
    public boolean saveIfAbsent(AssessmentProctoringAlert alert) {
        if (springDataAssessmentProctoringAlertRepository.existsByEventId(alert.getEventId())) {
            return false;
        }
        try {
            springDataAssessmentProctoringAlertRepository.saveAndFlush(AssessmentProctoringAlertMapper.toJpa(alert));
            return true;
        } catch (DataIntegrityViolationException ex) {
            return false;
        }
    }

    @Override
    public List<AssessmentProctoringAlert> findByExamSessionIdOrderByCapturedAt(UUID examSessionId) {
        return springDataAssessmentProctoringAlertRepository.findByExamSessionIdOrderByCapturedAtAsc(examSessionId)
            .stream()
            .map(AssessmentProctoringAlertMapper::toDomain)
            .toList();
    }

    /**
     * Danh sách loại nằm ở {@link AssessmentProctoringAlert#INTEGRITY_ALERT_TYPES} chứ không viết cứng ở
     * đây: đó là một quyết định nghiệp vụ ("cảnh báo nào khiến bài mất đáng tin"), không phải một chi
     * tiết lưu trữ. Adapter chỉ dịch nó thành một câu truy vấn.
     *
     * <p>Cố tình KHÔNG lọc thêm theo {@code level}: hai điều kiện thì bản ghi cũ (MULTIPLE_PERSONS
     * mang CRITICAL) và bản ghi mới (mang WARNING) lại rơi về hai nhánh khác nhau -- đúng vết nứt
     * đang phải vá.
     */
    @Override
    public boolean hasIntegrityAlert(UUID examSessionId) {
        return springDataAssessmentProctoringAlertRepository
            .existsIntegrityAlert(examSessionId, AssessmentProctoringAlert.INTEGRITY_ALERT_TYPES);
    }

    @Override
    public List<AssessmentProctoringAlert> findByScheduleIdOrderByCapturedAt(UUID scheduleId) {
        return springDataAssessmentProctoringAlertRepository.findByScheduleIdOrderByCapturedAtAsc(scheduleId)
            .stream()
            .map(AssessmentProctoringAlertMapper::toDomain)
            .toList();
    }
}
