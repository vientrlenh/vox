package com.sep.vox.infrastructure.persistence.entity.user;

import com.sep.vox.infrastructure.persistence.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "roles", indexes = {
    @Index(columnList = "code", name = "idx_role_code", unique = true),
    @Index(columnList = "name", name = "idx_role_name")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoleJpaEntity extends BaseEntity {

    @Column(name = "code", length = 100, nullable = false)
    private String code;

    @Column(name = "name", length = 255, nullable = false)
    private String name;
}
