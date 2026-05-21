package com.kkori.api.auth.service;

import com.kkori.api.user.entity.OAuthProvider;

public interface ProviderLogoutClient {

    boolean supports(OAuthProvider provider);

    boolean logout(String providerAccessToken);
}
