package com.kkori.api.pet.event;

import java.util.List;

public record PetImageCleanupEvent(List<ImageKey> imageKeys) {
    public record ImageKey(String petExternalId, String photoExternalId) {}
}
