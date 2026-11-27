package com.sep.vox.domain.model.school;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class SchoolRoomTests {

    @Test
    void registeredSetsEveryArgumentAndStartsActive() {
        UUID schoolId = UUID.randomUUID();

        SchoolRoom room = SchoolRoom.registered(schoolId, "R101", "Room 101", "Ground floor");

        assertThat(room.getId()).isNull();
        assertThat(room.getSchoolId()).isEqualTo(schoolId);
        assertThat(room.getCode()).isEqualTo("R101");
        assertThat(room.getName()).isEqualTo("Room 101");
        assertThat(room.getDescription()).isEqualTo("Ground floor");
        assertThat(room.isActive()).isTrue();
    }
}
