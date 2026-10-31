package com.projectii.Nei.repository;

import com.projectii.Nei.enums.RunStatus;
import com.projectii.Nei.model.ScheduleRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ScheduleRunRepository extends JpaRepository<ScheduleRun, UUID> {
    List<ScheduleRun> findByStatus(RunStatus status);
}
