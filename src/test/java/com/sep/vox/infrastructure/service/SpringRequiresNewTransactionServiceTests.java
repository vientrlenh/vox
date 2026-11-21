package com.sep.vox.infrastructure.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import java.util.function.Supplier;

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
import com.sep.vox.support.RepositoryTest;

@RepositoryTest
@Import(SpringRequiresNewTransactionService.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class SpringRequiresNewTransactionServiceTests {

    private static final String CODE_PREFIX = "TX";

    @Autowired
    private SpringRequiresNewTransactionService service;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void deleteInsertedRows() {
        jdbcTemplate.update("DELETE FROM supported_languages WHERE code LIKE ?", CODE_PREFIX + "%");
    }

    @Test
    void callInNewTransaction_commitsWorkEvenWhenTheEnclosingTransactionRollsBack() {
        TransactionTemplate outer = new TransactionTemplate(transactionManager);

        String result = outer.execute(status -> {
            String returned = service.callInNewTransaction(() -> insertLanguageAndReturn("TXA"));
            status.setRollbackOnly();
            return returned;
        });

        assertThat(result).isEqualTo("done");
        assertThat(countRows("TXA")).isEqualTo(1);
    }

    @Test
    void callInNewTransaction_rethrowsTheSameExceptionAndRollsBackOnlyTheInnerTransaction() {
        TransactionTemplate outer = new TransactionTemplate(transactionManager);
        IllegalStateException failure = new IllegalStateException("inner failure");

        outer.executeWithoutResult(status -> {
            insertLanguage("TXB");
            Supplier<String> failingWork = () -> {
                insertLanguage("TXC");
                throw failure;
            };
            assertThatThrownBy(() -> service.callInNewTransaction(failingWork)).isSameAs(failure);
        });

        assertThat(countRows("TXB")).isEqualTo(1);
        assertThat(countRows("TXC")).isZero();
    }

    @Test
    void runInNewTransaction_commitsWorkEvenWhenTheEnclosingTransactionRollsBack() {
        TransactionTemplate outer = new TransactionTemplate(transactionManager);

        outer.executeWithoutResult(status -> {
            service.runInNewTransaction(() -> insertLanguage("TXD"));
            status.setRollbackOnly();
        });

        assertThat(countRows("TXD")).isEqualTo(1);
    }

    @Test
    void runInNewTransaction_rethrowsTheSameExceptionAndRollsBackOnlyTheInnerTransaction() {
        TransactionTemplate outer = new TransactionTemplate(transactionManager);
        IllegalStateException failure = new IllegalStateException("inner failure");

        outer.executeWithoutResult(status -> {
            insertLanguage("TXE");
            Runnable failingWork = () -> {
                insertLanguage("TXF");
                throw failure;
            };
            assertThatThrownBy(() -> service.runInNewTransaction(failingWork)).isSameAs(failure);
        });

        assertThat(countRows("TXE")).isEqualTo(1);
        assertThat(countRows("TXF")).isZero();
    }

    @Test
    void runInNewTransaction_turnsAUniqueViolationRaisedByTheWorkIntoTheCarrier() {
        insertLanguage("TXG");

        assertThatThrownBy(() -> service.runInNewTransaction(() -> insertLanguage("TXG")))
            .isInstanceOf(DataConflictException.class);
    }

    @Test
    void runInNewTransaction_turnsAViolationRaisedAtTheInnerCommitIntoTheCarrier() {
        jdbcTemplate.execute("CREATE TABLE tx_deferred_unique (id int, "
            + "CONSTRAINT tx_deferred_unique_id UNIQUE (id) DEFERRABLE INITIALLY DEFERRED)");
        try {
            jdbcTemplate.update("INSERT INTO tx_deferred_unique (id) VALUES (1)");
            Runnable duplicateInsert = () -> jdbcTemplate.update("INSERT INTO tx_deferred_unique (id) VALUES (1)");

            assertThatThrownBy(() -> service.runInNewTransaction(duplicateInsert))
                .isInstanceOf(DataConflictException.class);
        } finally {
            jdbcTemplate.execute("DROP TABLE tx_deferred_unique");
        }
    }

    private String insertLanguageAndReturn(String code) {
        insertLanguage(code);
        return "done";
    }

    private void insertLanguage(String code) {
        jdbcTemplate.update(
            "INSERT INTO supported_languages (id, code, name, is_active, created_at, updated_at) "
                + "VALUES (?, ?, ?, true, now(), now())",
            UUID.randomUUID(), code, "Language " + code);
    }

    private int countRows(String code) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM supported_languages WHERE code = ?", Integer.class, code);
        return count == null ? 0 : count;
    }
}
