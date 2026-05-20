package com.kkori.api.auth.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kkori.api.common.exception.BusinessException;
import com.kkori.api.common.exception.ErrorCode;
import com.kkori.api.user.entity.OAuthProvider;
import com.kkori.api.user.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenIssuerVerifierTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final JwtProperties properties = new JwtProperties(
            "test-jwt-secret-test-jwt-secret-1234",
            3600,
            2592000
    );
    private final JwtTokenIssuer issuer = new JwtTokenIssuer(properties, objectMapper);
    private final JwtTokenVerifier verifier = new JwtTokenVerifier(properties, objectMapper);

    @Test
    void verifiesIssuedAccessToken() {
        String token = issuer.issueAccessToken(user());

        JwtClaims claims = verifier.verifyAccessToken(token);

        assertThat(claims.userId()).isEqualTo(1L);
        assertThat(claims.userExternalId()).isEqualTo("user-1");
        assertThat(claims.tokenType()).isEqualTo(JwtTokenType.ACCESS);
    }

    @Test
    void rejectsTamperedToken() {
        String token = issuer.issueAccessToken(user()) + "tampered";

        assertThatThrownBy(() -> verifier.verifyAccessToken(token))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(ErrorCode.AUTH_003));
    }

    @Test
    void rejectsRefreshTokenAsAccessToken() {
        String token = issuer.issueRefreshToken(user());

        assertThatThrownBy(() -> verifier.verifyAccessToken(token))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(ErrorCode.AUTH_003));
    }

    private User user() {
        User user = User.builder()
                .externalId("user-1")
                .provider(OAuthProvider.GOOGLE)
                .providerUserId("google-1")
                .build();
        ReflectionTestUtils.setField(user, "id", 1L);
        return user;
    }
}
