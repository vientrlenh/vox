package com.sep.vox.infrastructure.persistence.adapter;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.subscription.SubscriptionPlan;
import com.sep.vox.domain.model.subscription.SubscriptionPlanStatus;
import com.sep.vox.domain.repository.SubscriptionPlanRepository;
import com.sep.vox.infrastructure.persistence.entity.SubscriptionPlanJpaEntity;
import com.sep.vox.infrastructure.persistence.mapper.SubscriptionPlanMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataSubscriptionPlanRepository;

@Repository
public class SubscriptionPlanRepositoryImpl implements SubscriptionPlanRepository {

    // Gói mới lên đầu. Sắp theo id chứ không theo createdAt: id là uuidv7 do Postgres sinh
    // (V1__baseline.sql: "id uuid DEFAULT uuidv7()"), 48 bit đầu là mốc thời gian big-endian và
    // Postgres so sánh uuid theo thứ tự byte -- nên thứ tự id CHÍNH LÀ thứ tự tạo.
    //
    // Dùng id có hai cái lợi so với createdAt:
    //   - id là khóa chính nên DUY NHẤT, tự nó đã là thứ tự toàn phần. createdAt không duy nhất, phải
    //     kèm khóa phụ mới phân trang ổn định được -- thiếu nó thì một gói có thể hiện ở cả trang 1
    //     lẫn trang 2 còn gói khác không bao giờ xuất hiện.
    //   - createdAt KHÔNG có default ở DB mà do ứng dụng gán, nên nhiều gói tạo trong cùng một lượt
    //     gọi sẽ mang y hệt một Instant, trong khi uuidv7() nhích lên ở từng dòng.
    //
    // ĐÁNH ĐỔI: chỗ này phụ thuộc vào việc khóa chính còn là uuidv7. Đổi default sang gen_random_uuid()
    // (v4) thì thứ tự thành ngẫu nhiên mà không có lỗi biên dịch nào báo -- đổi thì phải quay lại
    // createdAt kèm id làm khóa phụ.
    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("id"));

    private final SpringDataSubscriptionPlanRepository springDataSubscriptionPlanRepository;

    public SubscriptionPlanRepositoryImpl(SpringDataSubscriptionPlanRepository springDataSubscriptionPlanRepository) {
        this.springDataSubscriptionPlanRepository = springDataSubscriptionPlanRepository;
    }

    @Override
    public Optional<SubscriptionPlan> findById(UUID id) {
        return springDataSubscriptionPlanRepository.findById(id).map(SubscriptionPlanMapper::toDomain);
    }

    @Override
    public SubscriptionPlan save(SubscriptionPlan plan) {
        var entity = SubscriptionPlanMapper.toJpa(plan);
        var saved = springDataSubscriptionPlanRepository.save(entity);
        return SubscriptionPlanMapper.toDomain(saved);
    }

    @Override
    public List<SubscriptionPlan> findByStatus(SubscriptionPlanStatus status) {
        return springDataSubscriptionPlanRepository.findByStatus(status.name()).stream()
            .map(SubscriptionPlanMapper::toDomain)
            .toList();
    }

    @Override
    public List<SubscriptionPlan> findByIdIn(Collection<UUID> ids) {
        return springDataSubscriptionPlanRepository.findAllById(ids).stream()
            .map(SubscriptionPlanMapper::toDomain)
            .toList();
    }

    @Override
    public void deleteById(UUID id) {
        springDataSubscriptionPlanRepository.deleteById(id);
    }

    @Override
    public boolean existsByReplacedByPlanId(UUID replacedByPlanId) {
        return springDataSubscriptionPlanRepository.existsByReplacedByPlanId(replacedByPlanId);
    }

    @Override
    public List<SubscriptionPlan> findByReplacedByPlanId(UUID replacedByPlanId) {
        return springDataSubscriptionPlanRepository.findByReplacedByPlanId(replacedByPlanId).stream()
            .map(SubscriptionPlanMapper::toDomain)
            .toList();
    }

    @Override
    public PageResult<SubscriptionPlan> findByStatus(SubscriptionPlanStatus status, int page, int size) {
        return toPageResult(
            // page vào theo lối 1-BASED như mọi repository khác trong dự án (xem OrderRepositoryImpl),
            // còn PageRequest đếm từ 0 -- thiếu phép trừ này thì trang đầu không cách nào lấy được.
            springDataSubscriptionPlanRepository.findByStatus(status.name(), PageRequest.of(page - 1, size, NEWEST_FIRST)),
            page,
            size
        );
    }

    @Override
    public PageResult<SubscriptionPlan> findAll(int page, int size) {
        return toPageResult(
            // 1-based vào, 0-based xuống PageRequest -- xem findByStatus.
            springDataSubscriptionPlanRepository.findAll(PageRequest.of(page - 1, size, NEWEST_FIRST)),
            page,
            size
        );
    }

    private PageResult<SubscriptionPlan> toPageResult(Page<SubscriptionPlanJpaEntity> result, int page, int size) {
        return new PageResult<>(
            result.getContent().stream().map(SubscriptionPlanMapper::toDomain).toList(),
            page,
            size,
            result.getTotalElements(),
            result.getTotalPages()
        );
    }
}
