package com.kkori.api.photo.storage;

import com.kkori.api.common.exception.BusinessException;
import com.kkori.api.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class S3PhotoStorageImpl implements S3PhotoStorage {

    private final S3Client s3Client;
    private final AwsS3Properties properties;

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png");
    private static final long MAX_MEDIUM_SIZE = 1024 * 1024L;       // 1MB
    private static final long MAX_THUMBNAIL_SIZE = 200 * 1024L;     // 200KB

    @Override
    public String uploadMedium(String petExternalId, String photoExternalId, MultipartFile file) {
        validateFile(file, MAX_MEDIUM_SIZE);
        String key = "photos/%s/%s/medium.jpg".formatted(petExternalId, photoExternalId);
        return upload(key, file);
    }

    @Override
    public String uploadThumbnail(String petExternalId, String photoExternalId, MultipartFile file) {
        validateFile(file, MAX_THUMBNAIL_SIZE);
        String key = "photos/%s/%s/thumb.jpg".formatted(petExternalId, photoExternalId);
        return upload(key, file);
    }

    @Override
    public void delete(String petExternalId, String photoExternalId) {
        deleteKey("photos/%s/%s/medium.jpg".formatted(petExternalId, photoExternalId));
        deleteKey("photos/%s/%s/thumb.jpg".formatted(petExternalId, photoExternalId));
    }

    private String upload(String key, MultipartFile file) {
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(properties.bucket())
                    .key(key)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();
            s3Client.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            return "https://%s.s3.%s.amazonaws.com/%s".formatted(properties.bucket(), properties.region(), key);
        } catch (IOException | S3Exception e) {
            log.error("S3 error", e);
            throw new BusinessException(ErrorCode.PHOTO_004);
        }
    }

    private void deleteKey(String key) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(properties.bucket())
                    .key(key)
                    .build());
        } catch (S3Exception e) {
            String errorCode = e.awsErrorDetails().errorCode();
            if (!"NoSuchKey".equals(errorCode)) {
                log.warn("S3 delete failed: key={}, errorCode={}", key, errorCode);
            }
        }
    }

    private void validateFile(MultipartFile file, long maxSize) {
        if (!ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
            throw new BusinessException(ErrorCode.PHOTO_005);
        }
        if (file.getSize() > maxSize) {
            throw new BusinessException(ErrorCode.PHOTO_006);
        }
    }
}
