package com.kkori.api.auth.oauth;

import com.kkori.api.common.exception.BusinessException;
import com.kkori.api.common.exception.ErrorCode;
import com.kkori.api.user.entity.OAuthProvider;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class OAuthVerifierResolver {

    private final Map<OAuthProvider, OAuthVerifier> verifiers = new EnumMap<>(OAuthProvider.class);

    public OAuthVerifierResolver(List<OAuthVerifier> verifiers) {
        verifiers.forEach(verifier -> this.verifiers.put(verifier.provider(), verifier));
    }

    public OAuthVerifier resolve(OAuthProvider provider) {
        OAuthVerifier verifier = verifiers.get(provider);
        if (verifier == null) {
            throw new BusinessException(ErrorCode.AUTH_001);
        }
        return verifier;
    }
}
