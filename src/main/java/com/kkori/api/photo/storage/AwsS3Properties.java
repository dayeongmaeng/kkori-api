package com.kkori.api.photo.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aws.s3")
public record AwsS3Properties(
        String bucket,
        String region,
        String accessKey,
        String secretKey
) {}
