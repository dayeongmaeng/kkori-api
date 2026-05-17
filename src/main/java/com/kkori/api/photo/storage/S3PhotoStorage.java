package com.kkori.api.photo.storage;

import org.springframework.web.multipart.MultipartFile;

public interface S3PhotoStorage {
    String uploadMedium(String petExternalId, String photoExternalId, MultipartFile file);
    String uploadThumbnail(String petExternalId, String photoExternalId, MultipartFile file);
    void delete(String petExternalId, String photoExternalId);
}
