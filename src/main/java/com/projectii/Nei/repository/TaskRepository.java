package com.projectii.Nei.repository;

import com.projectii.Nei.enums.TaskStatus;
import com.projectii.Nei.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface TaskRepository extends JpaRepository<Task, UUID> {
    List<Task> findByDateAndStatus(LocalDate today, TaskStatus taskStatus);

    List<Task> findByDate(LocalDate today);

    List<Task> findByRecurringTrueAndStatus(TaskStatus taskStatus);

    List<Task> findByDateBetweenOrderByDateAscStartTimeAsc(LocalDate from, LocalDate to);
}
