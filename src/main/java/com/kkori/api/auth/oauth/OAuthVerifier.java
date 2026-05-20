package com.kkori.api.auth.oauth;

import com.kkori.api.user.entity.OAuthProvider;

public interface OAuthVerifier {
    OAuthProvider provider();

    OAuthUserInfo verify(String token);
}
