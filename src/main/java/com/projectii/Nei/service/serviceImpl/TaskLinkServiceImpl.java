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

    @Override
    @Transactional
    public TaskLinkDto createLink(UUID fromId, UUID toId) {
        if (fromId.equals(toId))
            throw new IllegalArgumentException("A task can't thread to itself");
        if (taskLinkRepository.existsByFromTaskId(fromId))
            throw new IllegalStateException("This task already has a next task");
        if (taskLinkRepository.existsByToTaskId(toId))
            throw new IllegalStateException("That task already has a previous task");
        if (wouldLoop(fromId, toId))
            throw new IllegalArgumentException("That would make a loop");

        return saveLink(fromId, toId);
    }

    @Override
    @Transactional
    public TaskLinkDto relink(UUID fromId, UUID toId) {
        // checked against the graph as it is now: removing the old thread out of "from" or into "to"
        // can never break a path that runs to -> ... -> from, so this is safe to do first
        if (wouldLoop(fromId, toId))
            throw new IllegalArgumentException("That would make a loop");

        TaskLink out = taskLinkRepository.findByFromTaskId(fromId).orElse(null);
        TaskLink in = taskLinkRepository.findByToTaskId(toId).orElse(null);
        if (out != null) taskLinkRepository.delete(out);
        if (in != null && (out == null || !in.getId().equals(out.getId()))) taskLinkRepository.delete(in);
        taskLinkRepository.flush();   // deletes must hit the database before the insert (unique from/to pair)

        return saveLink(fromId, toId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean wouldLoop(UUID fromId, UUID toId) {
        // every task has at most one thread out, so just follow the chain that starts at "to"
        UUID cur = toId;
        for (int hops = 0; cur != null && hops < 10_000; hops++) {
            if (cur.equals(fromId)) return true;
            cur = taskLinkRepository.findByFromTaskId(cur).map(l -> l.getToTask().getId()).orElse(null);
        }
        return false;
    }

    @Override
    @Transactional
    public void deleteLink(UUID linkId) {
        taskLinkRepository.deleteById(linkId);
    }

    @Override
    @Transactional
    public void deleteLinkBetween(UUID fromId, UUID toId) {
        taskLinkRepository.findByFromTaskIdAndToTaskId(fromId, toId).ifPresent(taskLinkRepository::delete);
    }

    private TaskLinkDto saveLink(UUID fromId, UUID toId) {
        TaskLink link = new TaskLink();
        link.setFromTask(taskRepository.getReferenceById(fromId));
        link.setToTask(taskRepository.getReferenceById(toId));
        TaskLink saved = taskLinkRepository.save(link);
        return new TaskLinkDto(saved.getId(), fromId, toId);
    }
}
