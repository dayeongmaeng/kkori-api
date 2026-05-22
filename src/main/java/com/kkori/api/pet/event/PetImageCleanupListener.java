package com.kkori.api.pet.event;

import com.kkori.api.photo.storage.S3PhotoStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class PetImageCleanupListener {

    private final S3PhotoStorage s3PhotoStorage;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(PetImageCleanupEvent event) {
        for (PetImageCleanupEvent.ImageKey key : event.imageKeys()) {
            try {
                log.info("이미지 삭제: {}", key);
                s3PhotoStorage.delete(key.petExternalId(), key.photoExternalId());
            } catch (Exception e) {
                log.error("S3 이미지 삭제 실패 - petExternalId={}, photoExternalId={}",
                        key.petExternalId(), key.photoExternalId(), e);
            }
        }
    }
}
