package com.kkori.api.auth.oauth;

import com.kkori.api.auth.dto.request.OAuthLoginRequest;
import com.kkori.api.common.exception.BusinessException;
import com.kkori.api.common.exception.ErrorCode;
import com.kkori.api.user.entity.OAuthProvider;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

class KakaoOAuthVerifierTest {

    @Test
    void exchangesKakaoCodeWithRequestRedirectUriAndExtractsUserInfo() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        KakaoOAuthVerifier verifier = new KakaoOAuthVerifier(builder, properties());

        server.expect(requestTo("https://kauth.kakao.com/oauth/token"))
                .andExpect(method(POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(content().string(allOf(
                        containsString("grant_type=authorization_code"),
                        containsString("client_id=kakao-rest"),
                        containsString("redirect_uri=http%3A%2F%2Flocalhost%3A8081%2Foauth%2Fkakao"),
                        containsString("code=kakao-code")
                )))
                .andRespond(withSuccess("""
                        {
                          "access_token": "kakao-access-token"
                        }
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://kapi.kakao.com/v2/user/me"))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer kakao-access-token"))
                .andRespond(withSuccess("""
                        {
                          "id": 12345,
                          "kakao_account": {
                            "profile": {}
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        OAuthUserInfo userInfo = verifier.verify(new OAuthLoginRequest(
                OAuthProvider.KAKAO,
                null,
                null,
                "kakao-code",
                "http://localhost:8081/oauth/kakao",
                "device-1",
                null,
                null
        ));

        assertThat(userInfo.providerUserId()).isEqualTo("12345");
        assertThat(userInfo.email()).isNull();
        assertThat(userInfo.nickname()).isNull();
        server.verify();
    }

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
                new OAuthProperties.Google("google-client", null),
                new OAuthProperties.Kakao("kakao-rest", "kakao-native", null),
                null
        );
    }
}
