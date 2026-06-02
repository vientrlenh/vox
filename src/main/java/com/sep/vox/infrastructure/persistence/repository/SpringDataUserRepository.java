package com.sep.vox.infrastructure.persistence.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.infrastructure.persistence.entity.UserJpaEntity;

public interface SpringDataUserRepository extends JpaRepository<UserJpaEntity, UUID>{
    Optional<UserJpaEntity> findByEmail(String email);
    Optional<UserJpaEntity> findByPhone(String phone);
    boolean existsByEmail(String email);
    boolean existsByEmailAndStatus(String email, String status);

    @Modifying
    @Query("""
        UPDATE UserJpaEntity u 
        SET u.passwordHash = :passwordHash 
        WHERE u.email = :email 
            AND u.status = 'ACTIVE'
    """)
    int changeUserPassword(@Param("email") String email, @Param("passwordHash") String passwordHash);

    Optional<UserJpaEntity> findByEmailAndStatus(String email, String status);
    Optional<UserJpaEntity> findByIdAndStatus(UUID id, String status);
}
