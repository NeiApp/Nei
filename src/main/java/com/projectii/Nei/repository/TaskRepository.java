package com.projectii.Nei.repository;

import com.projectii.Nei.enums.TaskStatus;
import com.projectii.Nei.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
public interface TaskRepository extends JpaRepository<Task, UUID> {
    List<Task> findByDateAndStatus(LocalDate today, TaskStatus taskStatus);

    List<Task> findByDate(LocalDate today);

    List<Task> findByRecurringTrueAndStatus(TaskStatus taskStatus);

    List<Task> findByDateBetweenOrderByDateAscStartTimeAsc(LocalDate from, LocalDate to);

    // threaded tasks only: has a thread out, no thread in
    @Query("""
        select t from Task t
        where t.date = :date
          and exists     (select 1 from TaskLink l where l.fromTask = t)
          and not exists (select 1 from TaskLink l where l.toTask = t)
        order by t.startTime
        """)
    List<Task> findChainStarts(@Param("date") LocalDate date);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
    update Task t set t.status = :status
    where t.date = :date
      and (exists (select 1 from TaskLink l where l.fromTask = t)
        or exists (select 1 from TaskLink l where l.toTask = t))
    """)
    void resetStatusByDate(@Param("date") LocalDate date, @Param("status") TaskStatus status);
}
