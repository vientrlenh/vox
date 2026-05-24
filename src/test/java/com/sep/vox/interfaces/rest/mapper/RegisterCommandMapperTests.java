package com.sep.vox.interfaces.rest.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.sep.vox.interfaces.rest.dto.request.RegisterRequest;

public class RegisterCommandMapperTests {

    @Test
    void should_map_register_request_to_command() {
        var request = new RegisterRequest(
            "Nguyen Van A",
            "123456789",
            "0987654321",
            "admin@example.com",
            "24/05/2000",
            "123 Street",
            "school.edu.vn",
            "School Name",
            "456 School Street",
            "700000",
            "Principal",
            500
        );

        var command = RegisterCommandMapper.fromRequest(request);

        assertThat(command.contactFullName()).isEqualTo(request.contactFullName());
        assertThat(command.identityNumber()).isEqualTo(request.identityNumber());
        assertThat(command.contactPhone()).isEqualTo(request.contactPhone());
        assertThat(command.contactEmail()).isEqualTo(request.contactEmail());
        assertThat(command.dateOfBirth()).isEqualTo(LocalDate.of(2000, 5, 24));
        assertThat(command.contactAddress()).isEqualTo(request.contactAddress());
        assertThat(command.schoolDomain()).isEqualTo(request.schoolDomain());
        assertThat(command.schoolName()).isEqualTo(request.schoolName());
        assertThat(command.schoolAddress()).isEqualTo(request.schoolAddress());
        assertThat(command.postalCode()).isEqualTo(request.postalCode());
        assertThat(command.position()).isEqualTo(request.position());
        assertThat(command.studentCount()).isEqualTo(request.studentCount());
    }

    @Test
    void should_reject_request_when_date_format_is_invalid() {
        var request = new RegisterRequest(
            "Nguyen Van A",
            "123456789",
            "0987654321",
            "admin@example.com",
            "2000.05.24",
            "123 Street",
            "school.edu.vn",
            "School Name",
            "456 School Street",
            "700000",
            "Principal",
            500
        );

        assertThrows(IllegalArgumentException.class, () -> RegisterCommandMapper.fromRequest(request));
    }
}
