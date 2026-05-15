package com.kkori.api.photo.entity;

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
        name = "daily_photo",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_daily_photo_pet_date",
                columnNames = {"pet_id", "date"}
        )
)
public class DailyPhoto extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id", nullable = false, unique = true)
    private String externalId;

    @Column(name = "pet_id", nullable = false)
    private Long petId;

    @Column(name = "caregiver_id", nullable = false)
    private Long caregiverId;

    @Column(nullable = false)
    private LocalDate date;

    private String caption;

    @Builder
    public DailyPhoto(String externalId, Long petId, Long caregiverId,
                      LocalDate date, String caption) {
        this.externalId = externalId;
        this.petId = petId;
        this.caregiverId = caregiverId;
        this.date = date;
        this.caption = caption;
    }

    public void update(String caption) {
        this.caption = caption;
    }
}
