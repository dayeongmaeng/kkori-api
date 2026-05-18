package com.kkori.api.log.entity;

import com.kkori.api.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
@Entity
@Table(
        name = "daily_log_photo",
        indexes = {
                @Index(name = "idx_daily_log_photo_log_id", columnList = "daily_log_id"),
                @Index(name = "idx_daily_log_photo_pet_date", columnList = "pet_id,date")
        }
)
public class DailyLogPhoto extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id", nullable = false, unique = true)
    private String externalId;

    @Column(name = "daily_log_id", nullable = false)
    private Long dailyLogId;

    @Column(name = "pet_id", nullable = false)
    private Long petId;

    @Column(name = "caregiver_id", nullable = false)
    private Long caregiverId;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "medium_url", nullable = false)
    private String mediumUrl;

    @Column(name = "thumbnail_url", nullable = false)
    private String thumbnailUrl;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Builder
    public DailyLogPhoto(String externalId, Long dailyLogId, Long petId, Long caregiverId,
                         LocalDate date, String mediumUrl, String thumbnailUrl, Integer sortOrder) {
        this.externalId = externalId;
        this.dailyLogId = dailyLogId;
        this.petId = petId;
        this.caregiverId = caregiverId;
        this.date = date;
        this.mediumUrl = mediumUrl;
        this.thumbnailUrl = thumbnailUrl;
        this.sortOrder = sortOrder;
    }
}
