package com.sep.vox.infrastructure.persistence.adapter;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.rubric.RubricResultBand;
import com.sep.vox.domain.repository.RubricResultBandRepository;
import com.sep.vox.infrastructure.persistence.mapper.RubricResultBandMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataRubricResultBandRepository;

@Repository
public class RubricResultBandRepositoryImpl implements RubricResultBandRepository {

    private final SpringDataRubricResultBandRepository springDataRubricResultBandRepository;

    public RubricResultBandRepositoryImpl(SpringDataRubricResultBandRepository springDataRubricResultBandRepository) {
        this.springDataRubricResultBandRepository = springDataRubricResultBandRepository;
    }

    @Override
    public Optional<RubricResultBand> findById(UUID id) {
        return springDataRubricResultBandRepository.findById(id).map(RubricResultBandMapper::toDomain);
    }

    @Override
    public RubricResultBand save(RubricResultBand band) {
        var entity = RubricResultBandMapper.toJpa(band);
        var saved = springDataRubricResultBandRepository.save(entity);
        return RubricResultBandMapper.toDomain(saved);
    }
    
}
