package com.sep.vox.interfaces.shared;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletResponse;

class HttpCookieProviderTests {

    @Test
    void setCookie_should_write_http_only_cookie_with_configured_secure_and_same_site() {
        HttpCookieProvider provider = new HttpCookieProvider(true, "None");
        MockHttpServletResponse response = new MockHttpServletResponse();

        provider.setCookie(response, "refresh_token", "token-value", 259200L);

        assertThat(response.getHeaders(HttpHeaders.SET_COOKIE)).hasSize(1);
        assertThat(response.getHeader(HttpHeaders.SET_COOKIE))
            .startsWith("refresh_token=token-value;")
            .contains("Max-Age=259200", "Path=/", "Secure", "HttpOnly", "SameSite=None");
    }

    @Test
    void setCookie_should_omit_secure_when_not_configured_secure() {
        HttpCookieProvider provider = new HttpCookieProvider(false, "Lax");
        MockHttpServletResponse response = new MockHttpServletResponse();

        provider.setCookie(response, "refresh_token", "token-value", 60L);

        assertThat(response.getHeader(HttpHeaders.SET_COOKIE))
            .contains("Max-Age=60", "Path=/", "HttpOnly", "SameSite=Lax")
            .doesNotContain("Secure");
    }

    @Test
    void clearCookie_should_write_same_attributes_with_empty_value_and_zero_max_age() {
        HttpCookieProvider provider = new HttpCookieProvider(true, "None");
        MockHttpServletResponse response = new MockHttpServletResponse();

        provider.clearCookie(response, "refresh_token");

        assertThat(response.getHeader(HttpHeaders.SET_COOKIE))
            .startsWith("refresh_token=;")
            .contains("Max-Age=0", "Path=/", "Secure", "HttpOnly", "SameSite=None");
    }
}
