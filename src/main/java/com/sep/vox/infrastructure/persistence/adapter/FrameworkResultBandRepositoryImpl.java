package com.sep.vox.infrastructure.persistence.adapter;

import com.sep.vox.domain.model.framework.FrameworkResultBand;
import com.sep.vox.domain.repository.FrameworkResultBandRepository;
import com.sep.vox.infrastructure.persistence.mapper.FrameworkResultBandMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataFrameworkResultBandRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Repository
public class FrameworkResultBandRepositoryImpl implements FrameworkResultBandRepository {

    private final SpringDataFrameworkResultBandRepository springDataUserRepository;

    public FrameworkResultBandRepositoryImpl(SpringDataFrameworkResultBandRepository springDataFrameworkResultBandRepository) {
        this.springDataUserRepository = springDataFrameworkResultBandRepository;
    }


    @Override
    public Optional<FrameworkResultBand> findById(UUID id) {
        return springDataUserRepository.findById(id)
                .map(FrameworkResultBandMapper::toDomain);
    }


    @Override
    public List<FrameworkResultBand> findAllByIds(List<UUID> ids) {
       return springDataUserRepository.findAllById(ids)
                .stream()
                .map(FrameworkResultBandMapper::toDomain)
                .toList();
    }
}
