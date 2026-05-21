package com.kkori.api.auth.oauth;

import com.kkori.api.auth.dto.request.OAuthLoginRequest;
import com.kkori.api.user.entity.OAuthProvider;

public interface OAuthVerifier {
    OAuthProvider provider();

    OAuthUserInfo verify(String token);

    default OAuthUserInfo verify(OAuthLoginRequest request) {
        return verify(request.token());
    }
}
