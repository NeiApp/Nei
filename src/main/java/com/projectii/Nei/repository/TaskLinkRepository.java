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
    //Optional<TaskLink> findByFromTaskId(UUID fromTaskId);
    boolean existsByFromTaskId(UUID fromTaskId);
    boolean existsByToTaskId(UUID toTaskId);
}
