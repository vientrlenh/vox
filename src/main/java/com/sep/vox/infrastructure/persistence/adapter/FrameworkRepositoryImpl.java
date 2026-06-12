package com.sep.vox.infrastructure.persistence.adapter;

import com.sep.vox.domain.model.framework.Framework;
import com.sep.vox.domain.model.question.QuestionBank;
import com.sep.vox.domain.model.rubric.RubricVersion;
import com.sep.vox.domain.repository.FrameworkRepository;
import com.sep.vox.infrastructure.persistence.mapper.FrameworkMapper;
import com.sep.vox.infrastructure.persistence.mapper.QuestionBankMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataFrameworkRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class FrameworkRepositoryImpl implements FrameworkRepository {

    private final SpringDataFrameworkRepository springDataFrameworkRepository;

    public FrameworkRepositoryImpl(SpringDataFrameworkRepository springDataFrameworkRepository) {
        this.springDataFrameworkRepository = springDataFrameworkRepository;
    }

    @Override
    public Optional<Framework> findById(UUID id) {
        return springDataFrameworkRepository.findById(id)
                .map(FrameworkMapper ::toDomain);
    }
}
