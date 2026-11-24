package com.sep.vox.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThatThrownBy;


import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.sep.vox.application.exception.DataConflictException;
import com.sep.vox.domain.model.gradelevel.GradeLevel;
import com.sep.vox.domain.model.gradelevel.GradeLevelStatus;
import com.sep.vox.support.RepositoryTest;

@RepositoryTest
@Import(GradeLevelRepositoryImpl.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class GradeLevelRepositoryImplTests {

    private static final int FIRST_ORDER = 9001;
    private static final int SECOND_ORDER = 9002;

    @Autowired
    private GradeLevelRepositoryImpl gradeLevelRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @AfterEach
    void deleteInsertedRows() {
        jdbcTemplate.update("DELETE FROM grade_levels WHERE code LIKE 'DCTEST%'");
    }

    @Test
    void save_throwsDataConflictWhenTheOrderIsAlreadyUsed() {
        gradeLevelRepository.save(gradeLevel("DCTEST1", FIRST_ORDER));

        assertThatThrownBy(() -> gradeLevelRepository.save(gradeLevel("DCTEST2", FIRST_ORDER)))
            .isInstanceOf(DataConflictException.class);
    }

    @Test
    void updateGradeLevelAtomic_throwsDataConflictWhenTheOrderIsAlreadyUsed() {
        gradeLevelRepository.save(gradeLevel("DCTEST3", FIRST_ORDER));
        GradeLevel second = gradeLevelRepository.save(gradeLevel("DCTEST4", SECOND_ORDER));

        TransactionTemplate transaction = new TransactionTemplate(transactionManager);

        assertThatThrownBy(() -> transaction.executeWithoutResult(status -> gradeLevelRepository.updateGradeLevelAtomic(
            second.getId(), null, null, false, FIRST_ORDER)))
            .isInstanceOf(DataConflictException.class);
    }

    private GradeLevel gradeLevel(String code, int order) {
        return GradeLevel.builder().code(code).name("Data conflict test").order(order).status(GradeLevelStatus.ACTIVE).build();
    }
}
