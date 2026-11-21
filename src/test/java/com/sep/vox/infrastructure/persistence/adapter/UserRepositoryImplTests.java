package com.sep.vox.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.DataConflictException;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.support.RepositoryTest;

@RepositoryTest
@Import(UserRepositoryImpl.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class UserRepositoryImplTests {

    private static final String PHONE = "0900099999";

    @Autowired
    private UserRepositoryImpl userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void deleteInsertedRows() {
        jdbcTemplate.update("DELETE FROM users WHERE email LIKE 'dctest%'");
    }

    @Test
    void saveAndFlush_throwsDataConflictWhenThePhoneAlreadyExists() {
        userRepository.saveAndFlush(user("dctest1@example.com"));

        assertThatThrownBy(() -> userRepository.saveAndFlush(user("dctest2@example.com")))
            .isInstanceOf(DataConflictException.class);
    }

    private User user(String email) {
        return User.create(email, PHONE, "Data Conflict", LocalDate.of(2000, 1, 1), "Address", null);
    }
}
