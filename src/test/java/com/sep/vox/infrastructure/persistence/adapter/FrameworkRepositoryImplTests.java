package com.sep.vox.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.DataConflictException;
import com.sep.vox.domain.model.framework.Framework;
import com.sep.vox.domain.valueobject.FrameworkCode;
import com.sep.vox.support.RepositoryTest;

/**
 * Runs without an enclosing transaction so each save commits inside the adapter call and the unique violation
 * surfaces there, as it does for the atomic updates.
 */
@RepositoryTest
@Import(FrameworkRepositoryImpl.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class FrameworkRepositoryImplTests {

    private static final String CODE = "DCTEST";

    @Autowired
    private FrameworkRepositoryImpl frameworkRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void deleteInsertedRows() {
        jdbcTemplate.update("DELETE FROM frameworks WHERE code = ?", CODE);
    }

    @Test
    void save_throwsDataConflictWhenTheFrameworkCodeAlreadyExists() {
        frameworkRepository.save(framework());

        assertThatThrownBy(() -> frameworkRepository.save(framework())).isInstanceOf(DataConflictException.class);
    }

    private Framework framework() {
        return Framework.builder().code(new FrameworkCode(CODE)).name("Data conflict test").description("d").build();
    }
}
