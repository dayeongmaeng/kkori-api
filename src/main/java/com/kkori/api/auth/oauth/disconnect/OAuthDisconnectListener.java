package com.kkori.api.auth.oauth.disconnect;

import com.kkori.api.user.event.UserWithdrawalEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuthDisconnectListener {

    private final List<OAuthDisconnectService> disconnectServices;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(UserWithdrawalEvent event) {
        if (event.originalProvider() == null) {
            return;
        }
        log.info("[OAuth] disconnect requested: provider={}, userId={}",
                event.originalProvider(), event.userId());

        disconnectServices.stream()
                .filter(s -> s.supports(event.originalProvider()))
                .findFirst()
                .ifPresentOrElse(
                        service -> {
                            try {
                                boolean success = service.disconnect(event.userId(), event.originalProviderUserId());
                                if (!success) {
                                    log.warn("[OAuth] disconnect reported failure: provider={}, userId={}",
                                            event.originalProvider(), event.userId());
                                }
                            } catch (Exception e) {
                                log.error("[OAuth] disconnect threw exception: provider={}, userId={}",
                                        event.originalProvider(), event.userId(), e);
                            }
                        },
                        () -> log.info("[OAuth] no disconnect service registered: provider={}, userId={}",
                                event.originalProvider(), event.userId())
                );
    }
}
