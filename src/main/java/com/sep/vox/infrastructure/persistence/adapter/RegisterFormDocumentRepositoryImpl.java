package com.sep.vox.infrastructure.persistence.adapter;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.registerform.RegisterFormDocument;
import com.sep.vox.domain.repository.RegisterFormDocumentRepository;
import com.sep.vox.infrastructure.persistence.mapper.RegisterFormDocumentMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataRegisterFormDocumentRepository;

@Repository
public class RegisterFormDocumentRepositoryImpl implements RegisterFormDocumentRepository {

    private final SpringDataRegisterFormDocumentRepository springDataRegisterFormDocumentRepository;

    public RegisterFormDocumentRepositoryImpl(SpringDataRegisterFormDocumentRepository springDataRegisterFormDocumentRepository) {
        this.springDataRegisterFormDocumentRepository = springDataRegisterFormDocumentRepository;
    }

    @Override
    public Optional<RegisterFormDocument> findById(UUID id) {
        return springDataRegisterFormDocumentRepository.findById(id)
            .map(RegisterFormDocumentMapper::toDomain);
    }

    @Override
    public RegisterFormDocument save(RegisterFormDocument document) {
        var entity = RegisterFormDocumentMapper.toJpa(document);
        var saved = springDataRegisterFormDocumentRepository.save(entity);
        return RegisterFormDocumentMapper.toDomain(saved);
    }

    @Override
    public List<RegisterFormDocument> findByRegisterFormId(UUID registerFormId) {
        return springDataRegisterFormDocumentRepository.findByRegisterFormId(registerFormId)
            .stream()
            .map(RegisterFormDocumentMapper::toDomain)
            .toList();
    }

    @Override
    public List<RegisterFormDocument> saveAll(Collection<RegisterFormDocument> documents) {
        var entities = documents.stream()
            .map(RegisterFormDocumentMapper::toJpa)
            .toList();
        var saved = springDataRegisterFormDocumentRepository.saveAll(entities);
        return saved.stream().map(RegisterFormDocumentMapper::toDomain).toList();
    }
    
}
