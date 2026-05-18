package com.kkori.api.photo.dto.response;

import com.kkori.api.photo.entity.DailyPhoto;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class DailyPhotoResponseTest {

    @Test
    void editedIsTrueWhenUpdatedAtIsAfterCreatedAt() {
        DailyPhoto photo = DailyPhoto.builder()
                .externalId("photo-1")
                .petId(1L)
                .caregiverId(1L)
                .date(LocalDate.of(2026, 5, 18))
                .caption("caption")
                .build();
        LocalDateTime createdAt = LocalDateTime.of(2026, 5, 18, 10, 0);
        ReflectionTestUtils.setField(photo, "createdAt", createdAt);
        ReflectionTestUtils.setField(photo, "updatedAt", createdAt.plusMinutes(1));

        DailyPhotoResponse response = DailyPhotoResponse.from(photo);

        assertThat(response.edited()).isTrue();
    }
}
