package com.sep.vox.infrastructure.properties;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AvatarPropertiesTests {

    @Test
    void should_return_empty_string_when_key_is_missing() {
        assertThat(new AvatarProperties(null).allowedHosts()).isEmpty();
    }

    @Test
    void should_return_configured_hosts_unchanged() {
        String hosts = "firebasestorage.googleapis.com, cdn.example.com";

        assertThat(new AvatarProperties(hosts).allowedHosts()).isEqualTo(hosts);
    }
}
