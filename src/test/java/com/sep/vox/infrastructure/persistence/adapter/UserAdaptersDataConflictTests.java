package com.sep.vox.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;


import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import com.sep.vox.application.exception.DataConflictException;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.infrastructure.persistence.repository.SpringDataSchoolUserRepository;
import com.sep.vox.infrastructure.persistence.repository.SpringDataUserRepository;

/**
 * Every Spring Data call below answers with a constraint violation. The adapter must translate it into the carrier
 * exception that the use cases catch.
 */
class UserAdaptersDataConflictTests {

    private static final DataIntegrityViolationException VIOLATION = new DataIntegrityViolationException("unique violation");

    private static <T> T repositoryRejectingEveryCall(Class<T> repositoryType) {
        return mock(repositoryType, invocation -> {
            throw VIOLATION;
        });
    }

    private static void assertCarrier(org.assertj.core.api.ThrowableAssert.ThrowingCallable call) {
        assertThatThrownBy(call).isInstanceOf(DataConflictException.class).hasCause(VIOLATION);
    }


    @Test
    void user_translatesTheConstraintViolation() {
        UserRepositoryImpl adapter = new UserRepositoryImpl(repositoryRejectingEveryCall(SpringDataUserRepository.class));
        assertCarrier(() -> adapter.save(User.builder().status(UserStatus.ACTIVE).build()));
        assertCarrier(() -> adapter.saveAndFlush(User.builder().status(UserStatus.ACTIVE).build()));
    }

    @Test
    void schoolUser_translatesTheConstraintViolation() {
        SchoolUserRepositoryImpl adapter = new SchoolUserRepositoryImpl(repositoryRejectingEveryCall(SpringDataSchoolUserRepository.class));
        assertCarrier(() -> adapter.save(SchoolUser.builder().build()));
    }
}
