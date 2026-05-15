package com.kkori.api.device.repository;

import com.kkori.api.device.entity.Device;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DeviceRepository extends JpaRepository<Device, Long> {
    Optional<Device> findByExternalId(String externalId);
}
