package com.projectii.Nei.service.serviceImpl;

import com.projectii.Nei.model.TaskLink;
import com.projectii.Nei.payload.response.TaskLinkDto;
import com.projectii.Nei.repository.TaskLinkRepository;
import com.projectii.Nei.repository.TaskRepository;
import com.projectii.Nei.service.TaskLinkService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TaskLinkServiceImpl implements TaskLinkService {
    private final TaskRepository taskRepository;
    private final TaskLinkRepository taskLinkRepository;
    private final SchedulerMapper schedulerMapper;

    @Override
    public TaskLinkDto createLink(UUID fromId, UUID toId){
        if (fromId.equals(toId))
            throw new IllegalArgumentException("A task can't thread to itself");
        if (taskLinkRepository.existsByFromTaskId(fromId))
            throw new IllegalStateException("This task already has a next task");
        if (taskLinkRepository.existsByToTaskId(toId))
            throw new IllegalStateException("That task already has a previous task");

        TaskLink taskLink = new TaskLink();
        taskLink.setFromTask(taskRepository.getReferenceById(fromId));
        taskLink.setToTask(taskRepository.getReferenceById(toId));

       return  new TaskLinkDto(taskLink.getFromTask().getId(),taskLink.getToTask().getId());
    }



    @Override
    @Transactional
    public void deleteLink(UUID linkId) {
        taskLinkRepository.deleteById(linkId);
    }
}
