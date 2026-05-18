package com.kkori.api.photo.dto.request;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@JsonTest
class UpdateDailyPhotoRequestTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void rejectsPhotoUrls() {
        String json = """
                {
                  "caption": "new caption",
                  "mediumUrl": "https://example.com/changed.jpg"
                }
                """;

        assertThatThrownBy(() -> objectMapper.readValue(json, UpdateDailyPhotoRequest.class))
                .isInstanceOf(Exception.class);
    }
}
