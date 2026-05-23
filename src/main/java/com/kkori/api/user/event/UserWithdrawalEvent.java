package com.kkori.api.user.event;

import com.kkori.api.user.entity.OAuthProvider;

// Provider 정보는 withdraw() 호출 전에 캡처해야 한다. withdraw() 후에는 null로 익명화됨.
public record UserWithdrawalEvent(
        Long userId,
        OAuthProvider originalProvider,
        String originalProviderUserId
) {}
