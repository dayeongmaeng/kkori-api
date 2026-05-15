package com.kkori.api.log.entity;

import com.kkori.api.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor
@Entity
@Table(
        name = "daily_log",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_daily_log_pet_date",
                columnNames = {"pet_id", "date"}
        )
)
public class DailyLog extends BaseEntity {

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

    @Enumerated(EnumType.STRING)
    private MealAmount meal;

    @Enumerated(EnumType.STRING)
    private WaterAmount water;

    private Integer walkMinutes;

    @Enumerated(EnumType.STRING)
    private StoolCondition pooCondition;

    @Enumerated(EnumType.STRING)
    private UrineColor urineColor;

    private Integer condition;

    @Column(precision = 5, scale = 2)
    private BigDecimal weightKg;

    @Column(columnDefinition = "TEXT")
    private String memo;

    @ElementCollection
    @CollectionTable(
            name = "daily_log_photo",
            joinColumns = @JoinColumn(name = "daily_log_id")
    )
    @Column(name = "photo_base64", columnDefinition = "TEXT")
    private List<String> photoBase64List = new ArrayList<>();

    @Builder
    public DailyLog(String externalId, Long petId, Long caregiverId, LocalDate date,
                    MealAmount meal, WaterAmount water, Integer walkMinutes,
                    StoolCondition pooCondition, UrineColor urineColor,
                    Integer condition, BigDecimal weightKg, String memo,
                    List<String> photoBase64List) {
        this.externalId = externalId;
        this.petId = petId;
        this.caregiverId = caregiverId;
        this.date = date;
        this.meal = meal;
        this.water = water;
        this.walkMinutes = walkMinutes;
        this.pooCondition = pooCondition;
        this.urineColor = urineColor;
        this.condition = condition;
        this.weightKg = weightKg;
        this.memo = memo;
        if (photoBase64List != null) {
            this.photoBase64List = photoBase64List;
        }
    }
}
