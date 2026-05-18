package com.kkori.api.photo.dto.request;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = false)
public record UpdateDailyPhotoRequest(
        String caption
) {
    @JsonAnySetter
    public void rejectUnknownFields(String fieldName, Object value) {
        throw new IllegalArgumentException("Unsupported field: " + fieldName);
    }
}
