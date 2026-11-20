package com.projectii.Nei.repository;

import com.projectii.Nei.enums.RunStatus;
import com.projectii.Nei.model.ScheduleRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ScheduleRunRepository extends JpaRepository<ScheduleRun, UUID> {
    List<ScheduleRun> findByStatus(RunStatus status);

    /** A run may point at a task that is about to be deleted: let go of it first (foreign key). */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update ScheduleRun r set r.currentTask = null where r.currentTask.id = :taskId")
    void clearCurrentTask(@Param("taskId") UUID taskId);
}
