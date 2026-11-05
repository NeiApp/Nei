package com.projectii.Nei.repository;

import com.projectii.Nei.model.Settings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SettingRepository extends JpaRepository<Settings, UUID> {
}
