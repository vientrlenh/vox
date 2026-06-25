package com.sep.vox.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.registerform.RegisterFormDocument;

public interface RegisterFormDocumentRepository {
    Optional<RegisterFormDocument> findById(UUID id);
    RegisterFormDocument save(RegisterFormDocument document);
    List<RegisterFormDocument> findByRegisterFormId(UUID registerFormId);
    List<RegisterFormDocument> saveAll(Collection<RegisterFormDocument> documents);
    List<RegisterFormDocument> findByRegisterFormIdIn(Collection<UUID> registerFormIds);
}
