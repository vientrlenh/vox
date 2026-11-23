package com.sep.vox.infrastructure.persistence.adapter;

import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.assessment.AssessmentProctoringAlert;
import com.sep.vox.domain.repository.AssessmentProctoringAlertRepository;
import com.sep.vox.infrastructure.persistence.mapper.AssessmentProctoringAlertMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataAssessmentProctoringAlertRepository;

@Repository
@RequiredArgsConstructor
public class AssessmentProctoringAlertRepositoryImpl implements AssessmentProctoringAlertRepository {

    private final SpringDataAssessmentProctoringAlertRepository springDataAssessmentProctoringAlertRepository;

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
    public List<AssessmentProctoringAlert> findByAssessmentSessionIdOrderByCapturedAt(UUID assessmentSessionId) {
        return springDataAssessmentProctoringAlertRepository.findByAssessmentSessionIdOrderByCapturedAtAsc(assessmentSessionId)
            .stream()
            .map(AssessmentProctoringAlertMapper::toDomain)
            .toList();
    }

    /**
     * The list of types lives in {@link AssessmentProctoringAlert#INTEGRITY_ALERT_TYPES} and is not hard-coded
     * here: it is a business decision ("which alerts make an attempt untrustworthy"), not a storage
     * detail. The adapter only translates it into a query.
     *
     * <p>Deliberately does NOT filter further by {@code level}: with two conditions, old records (MULTIPLE_PERSONS
     * carrying CRITICAL) and new records (carrying WARNING) would fall into two different branches -- exactly the crack
     * being patched.
     */
    @Override
    public boolean hasIntegrityAlert(UUID assessmentSessionId) {
        return springDataAssessmentProctoringAlertRepository
            .existsIntegrityAlert(assessmentSessionId, AssessmentProctoringAlert.INTEGRITY_ALERT_TYPES);
    }

    @Override
    public List<AssessmentProctoringAlert> findByScheduleIdOrderByCapturedAt(UUID scheduleId) {
        return springDataAssessmentProctoringAlertRepository.findByScheduleIdOrderByCapturedAtAsc(scheduleId)
            .stream()
            .map(AssessmentProctoringAlertMapper::toDomain)
            .toList();
    }
}
