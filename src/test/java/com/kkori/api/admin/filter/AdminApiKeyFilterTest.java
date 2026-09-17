package com.kkori.api.admin.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kkori.api.admin.config.AdminProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class AdminApiKeyFilterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void validApiKeyContinuesFilterChain() throws Exception {
        AdminApiKeyFilter filter = new AdminApiKeyFilter(new AdminProperties("secret-key"), objectMapper);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/internal/admin/members");
        request.addHeader("X-Admin-Api-Key", "secret-key");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean[] continued = {false};
        filter.doFilter(request, response, (req, res) -> continued[0] = true);

        assertThat(continued[0]).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void missingApiKeyReturns401() throws Exception {
        AdminApiKeyFilter filter = new AdminApiKeyFilter(new AdminProperties("secret-key"), objectMapper);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/internal/admin/members");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {
            throw new AssertionError("filter chain should not continue");
        });

        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    void wrongApiKeyReturns401() throws Exception {
        AdminApiKeyFilter filter = new AdminApiKeyFilter(new AdminProperties("secret-key"), objectMapper);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/internal/admin/members");
        request.addHeader("X-Admin-Api-Key", "wrong-key");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {
            throw new AssertionError("filter chain should not continue");
        });

        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    void unconfiguredApiKeyRejectsEvenWithHeader() throws Exception {
        AdminApiKeyFilter filter = new AdminApiKeyFilter(new AdminProperties(""), objectMapper);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/internal/admin/members");
        request.addHeader("X-Admin-Api-Key", "anything");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {
            throw new AssertionError("filter chain should not continue");
        });

        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    void nonAdminPathBypassesFilter() throws Exception {
        AdminApiKeyFilter filter = new AdminApiKeyFilter(new AdminProperties("secret-key"), objectMapper);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/health");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean[] continued = {false};
        filter.doFilter(request, response, (req, res) -> continued[0] = true);

        assertThat(continued[0]).isTrue();
    }
}
