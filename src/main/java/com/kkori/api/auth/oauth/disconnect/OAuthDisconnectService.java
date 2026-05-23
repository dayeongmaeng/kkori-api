package com.kkori.api.auth.oauth.disconnect;

import com.kkori.api.user.entity.OAuthProvider;

public interface OAuthDisconnectService {

    boolean supports(OAuthProvider provider);

    /**
     * OAuth 계정 연결 해제를 시도한다.
     * 구현체는 실패 시 예외 대신 false를 반환하거나 예외를 throw할 수 있으며,
     * 호출 측(OAuthDisconnectListener)에서 예외를 catch하여 로그만 남긴다.
     */
    boolean disconnect(Long userId, String providerUserId);
}
