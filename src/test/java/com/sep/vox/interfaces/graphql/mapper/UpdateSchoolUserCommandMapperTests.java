package com.sep.vox.interfaces.graphql.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.sep.vox.application.port.input.command.schooluser.UpdateSchoolUserCommand;

class UpdateSchoolUserCommandMapperTests {

    private final UUID schoolId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @Test
    void should_pass_a_coerced_date_of_birth_through_as_provided() {
        UpdateSchoolUserCommand command = UpdateSchoolUserCommandMapper.fromInput(
            schoolId, userId, Map.of("dateOfBirth", LocalDate.of(2008, 5, 1)));

        assertThat(command.dateOfBirth()).isEqualTo(LocalDate.of(2008, 5, 1));
        assertThat(command.dateOfBirthProvided()).isTrue();
    }

    @Test
    void should_mark_an_explicit_null_date_of_birth_as_provided() {
        Map<String, Object> input = new HashMap<>();
        input.put("dateOfBirth", null);

        UpdateSchoolUserCommand command = UpdateSchoolUserCommandMapper.fromInput(schoolId, userId, input);

        assertThat(command.dateOfBirth()).isNull();
        assertThat(command.dateOfBirthProvided()).isTrue();
    }

    @Test
    void should_mark_an_absent_date_of_birth_as_not_provided() {
        UpdateSchoolUserCommand command = UpdateSchoolUserCommandMapper.fromInput(schoolId, userId, Map.of());

        assertThat(command.dateOfBirth()).isNull();
        assertThat(command.dateOfBirthProvided()).isFalse();
    }
}
