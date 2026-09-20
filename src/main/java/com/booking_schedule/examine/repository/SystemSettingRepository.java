package com.booking_schedule.examine.repository;

import com.booking_schedule.examine.entity.SystemSetting;
import com.booking_schedule.examine.enums.ESettingKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SystemSettingRepository extends JpaRepository<SystemSetting, Long> {
    Optional<SystemSetting> findBySettingKey(ESettingKey settingKey);
}