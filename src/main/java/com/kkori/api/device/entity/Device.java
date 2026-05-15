package com.kkori.api.device.entity;

import com.kkori.api.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "device")
public class Device extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id", nullable = false, unique = true)
    private String externalId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Platform platform;

    @Column(name = "user_id")
    private Long userId;

    @Builder
    public Device(String externalId, Platform platform, Long userId) {
        this.externalId = externalId;
        this.platform = platform;
        this.userId = userId;
    }
}
