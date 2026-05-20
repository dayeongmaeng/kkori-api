package com.kkori.api.auth.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kkori.api.auth.context.AuthContext;
import com.kkori.api.auth.jwt.JwtClaims;
import com.kkori.api.auth.jwt.JwtTokenType;
import com.kkori.api.auth.jwt.JwtTokenVerifier;
import com.kkori.api.common.exception.BusinessException;
import com.kkori.api.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class JwtAuthenticationFilterTest {

    private final JwtTokenVerifier verifier = mock(JwtTokenVerifier.class);
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(verifier, new ObjectMapper());

    @Test
    void validAccessTokenSetsAuthContextAndRequestAttributes() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/pets");
        request.addHeader("Authorization", "Bearer access-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(verifier.verifyAccessToken("access-token"))
                .thenReturn(new JwtClaims(1L, "user-1", JwtTokenType.ACCESS));

        filter.doFilter(request, response, (req, res) -> {
            assertThat(AuthContext.currentUser()).isPresent();
            assertThat(AuthContext.currentUser().get().userId()).isEqualTo(1L);
            assertThat(req.getAttribute("userId")).isEqualTo(1L);
            assertThat(req.getAttribute("userExternalId")).isEqualTo("user-1");
        });

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(AuthContext.currentUser()).isEmpty();
    }

    @Test
    void missingTokenFallsBackToExistingDeviceFlow() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/pets");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> assertThat(AuthContext.currentUser()).isEmpty());

        verifyNoInteractions(verifier);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void invalidTokenReturns401() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/pets");
        request.addHeader("Authorization", "Bearer broken");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(verifier.verifyAccessToken("broken")).thenThrow(new BusinessException(ErrorCode.AUTH_003));

        filter.doFilter(request, response, (req, res) -> {
            throw new AssertionError("filter chain should not continue");
        });

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("\"success\":false");
        assertThat(response.getContentAsString()).contains("AUTH_003");
    }
}
