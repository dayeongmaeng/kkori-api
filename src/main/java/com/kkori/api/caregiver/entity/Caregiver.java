package com.kkori.api.caregiver.entity;

import com.kkori.api.common.entity.SoftDeletableEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Entity
@Table(
        name = "caregiver",
        indexes = @Index(name = "idx_caregiver_device_id", columnList = "device_id")
)
public class Caregiver extends SoftDeletableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id", nullable = false, unique = true)
    private String externalId;

    @Column(name = "device_id", nullable = false)
    private Long deviceId;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CaregiverRole role;

    @Column(nullable = false)
    private String color;

    @Builder
    public Caregiver(String externalId, Long deviceId, String name, CaregiverRole role, String color) {
        this.externalId = externalId;
        this.deviceId = deviceId;
        this.name = name;
        this.role = role;
        this.color = color;
    }

    public void update(String name, CaregiverRole role, String color) {
        this.name = name;
        this.role = role;
        this.color = color;
    }
}
