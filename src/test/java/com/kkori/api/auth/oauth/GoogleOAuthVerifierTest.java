package com.kkori.api.auth.oauth;

import com.kkori.api.common.exception.BusinessException;
import com.kkori.api.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GoogleOAuthVerifierTest {

    @Test
    void verifiesGoogleIdTokenAndExtractsUserInfo() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        GoogleOAuthVerifier verifier = new GoogleOAuthVerifier(builder, properties("google-client-id"));
        server.expect(requestTo("https://oauth2.googleapis.com/tokeninfo?id_token=google-token"))
                .andRespond(withSuccess("""
                        {
                          "sub": "google-user-1",
                          "aud": "google-client-id",
                          "email": "a@example.com",
                          "name": "Kkori",
                          "picture": "https://example.com/profile.png"
                        }
                        """, MediaType.APPLICATION_JSON));

        OAuthUserInfo userInfo = verifier.verify("google-token");

        assertThat(userInfo.providerUserId()).isEqualTo("google-user-1");
        assertThat(userInfo.email()).isEqualTo("a@example.com");
        assertThat(userInfo.nickname()).isEqualTo("Kkori");
        assertThat(userInfo.profileImageUrl()).isEqualTo("https://example.com/profile.png");
        server.verify();
    }

    @Test
    void rejectsGoogleIdTokenWithDifferentAudience() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        GoogleOAuthVerifier verifier = new GoogleOAuthVerifier(builder, properties("google-client-id"));
        server.expect(requestTo("https://oauth2.googleapis.com/tokeninfo?id_token=google-token"))
                .andRespond(withSuccess("""
                        {
                          "sub": "google-user-1",
                          "aud": "other-client-id"
                        }
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> verifier.verify("google-token"))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(ErrorCode.AUTH_002));
        server.verify();
    }

    private OAuthProperties properties(String googleClientId) {
        return new OAuthProperties(
                new OAuthProperties.Google(googleClientId),
                new OAuthProperties.Kakao("kakao-rest", "kakao-native")
        );
    }
}
