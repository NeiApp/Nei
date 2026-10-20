package com.projectii.Nei.repository;

import com.projectii.Nei.model.DayLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface DayLogRepository extends JpaRepository<DayLog, UUID> {
}
