package com.sep.vox.infrastructure.persistence.adapter;

import com.sep.vox.domain.model.framework.FrameworkCriterion;
import com.sep.vox.domain.repository.FrameworkCriterionRepository;
import com.sep.vox.infrastructure.persistence.mapper.FrameworkCriterionMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataFrameworkCriterionRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
public class FrameworkCriterionRepositoryImpl implements FrameworkCriterionRepository {

    private final SpringDataFrameworkCriterionRepository springDataFrameworkCriterionRepository;

    public FrameworkCriterionRepositoryImpl(SpringDataFrameworkCriterionRepository springDataFrameworkCriterionRepository) {
        this.springDataFrameworkCriterionRepository = springDataFrameworkCriterionRepository;
    }

    @Override
    public List<FrameworkCriterion> findAllByIds(List<UUID> ids) {
        // Trả về danh sách rỗng nếu danh sách ID truyền vào bị null hoặc rỗng
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }

        // Gọi hàm findAllById mặc định của JpaRepository và Map sang Domain
        return springDataFrameworkCriterionRepository.findAllById(ids)
                .stream()
                .map(FrameworkCriterionMapper::toDomain)
                .collect(Collectors.toList());
    }
}
