package com.projectii.Nei.repository;
import com.projectii.Nei.model.Task;
import com.projectii.Nei.model.TaskLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TaskLinkRepository extends JpaRepository<TaskLink, UUID> {
    Optional<TaskLink> findByFromTask(Task fromTask);
    Optional<TaskLink> findByFromTaskId(UUID fromTaskId);
    Optional<TaskLink> findByToTaskId(UUID toTaskId);
    Optional<TaskLink> findByFromTaskIdAndToTaskId(UUID fromTaskId, UUID toTaskId);
    boolean existsByFromTaskId(UUID fromTaskId);
    boolean existsByToTaskId(UUID toTaskId);

    /** Used when a task is deleted: threads in or out of it have to go first (foreign keys). */
    void deleteByFromTaskIdOrToTaskId(UUID fromTaskId, UUID toTaskId);
}
