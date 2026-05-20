package com.kkori.api.auth.oauth;

import com.kkori.api.common.exception.BusinessException;
import com.kkori.api.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

class KakaoOAuthVerifierTest {

    @Test
    void verifiesKakaoAccessTokenAndExtractsUserInfo() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        KakaoOAuthVerifier verifier = new KakaoOAuthVerifier(builder, properties());
        server.expect(requestTo("https://kapi.kakao.com/v2/user/me"))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer kakao-token"))
                .andRespond(withSuccess("""
                        {
                          "id": 12345,
                          "kakao_account": {
                            "email": "a@example.com",
                            "profile": {
                              "nickname": "Kkori",
                              "profile_image_url": "https://example.com/profile.png"
                            }
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        OAuthUserInfo userInfo = verifier.verify("kakao-token");

        assertThat(userInfo.providerUserId()).isEqualTo("12345");
        assertThat(userInfo.email()).isEqualTo("a@example.com");
        assertThat(userInfo.nickname()).isEqualTo("Kkori");
        assertThat(userInfo.profileImageUrl()).isEqualTo("https://example.com/profile.png");
        server.verify();
    }

    @Test
    void rejectsKakaoAccessTokenWhenUserInfoApiFails() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        KakaoOAuthVerifier verifier = new KakaoOAuthVerifier(builder, properties());
        server.expect(requestTo("https://kapi.kakao.com/v2/user/me"))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer kakao-token"))
                .andRespond(withStatus(UNAUTHORIZED));

        assertThatThrownBy(() -> verifier.verify("kakao-token"))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(ErrorCode.AUTH_002));
        server.verify();
    }

    private OAuthProperties properties() {
        return new OAuthProperties(
                new OAuthProperties.Google("google-client"),
                new OAuthProperties.Kakao("kakao-rest", "kakao-native")
        );
    }
}
