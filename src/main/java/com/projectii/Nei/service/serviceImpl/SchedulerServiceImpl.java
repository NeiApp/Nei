package com.projectii.Nei.service.serviceImpl;

import com.projectii.Nei.enums.RunStatus;
import com.projectii.Nei.enums.TaskStatus;
import com.projectii.Nei.model.ScheduleRun;
import com.projectii.Nei.model.Task;
import com.projectii.Nei.model.TaskLink;
import com.projectii.Nei.payload.response.RunStateDto;
import com.projectii.Nei.repository.ScheduleRunRepository;
import com.projectii.Nei.repository.TaskLinkRepository;
import com.projectii.Nei.repository.TaskRepository;
import com.projectii.Nei.service.SchedulerService;
import com.projectii.Nei.service.utilityService.TaskReminderEvent;
import com.projectii.Nei.service.utilityService.TaskStartedEvent;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SchedulerServiceImpl implements SchedulerService {
    private final ScheduleRunRepository runRepo;
    private final TaskRepository taskRepo;
    private final TaskLinkRepository linkRepo;
    private final ApplicationEventPublisher events;

    @Override
    @Transactional
    public RunStateDto play(LocalDate date) {
        runRepo.findByStatus(RunStatus.RUNNING).stream()
                .filter(r -> r.getDate().equals(date))
                .forEach(r -> r.setStatus(RunStatus.STOPPED));          // never two live runs

        Task first = taskRepo.findChainStarts(date).stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("Thread some tasks first"));

        ScheduleRun run = new ScheduleRun();
        run.setDate(date);
        run.setStatus(RunStatus.RUNNING);
        run.setCurrentTask(first);
        run.setLastReminderAt(Instant.now());
        return toState(runRepo.save(run));
    }

    @Override
    @Transactional
    public RunStateDto finishCurrent(UUID runId) {
        ScheduleRun run = runRepo.findById(runId).orElseThrow();
        Task done = run.getCurrentTask();
        if (run.getStatus() != RunStatus.RUNNING || done == null)
            throw new IllegalStateException("No active task to finish");

        done.setStatus(TaskStatus.DONE);
        Optional<Task> next = linkRepo.findByFromTask(done).map(TaskLink::getToTask);
        run.setCurrentTask(next.orElse(null));
        run.setLastReminderAt(Instant.now());

        if (next.isEmpty() && nextChainStart(run.getDate()).isEmpty())
            run.setStatus(RunStatus.COMPLETED);
        return toState(run);
    }

    @Override
    @Transactional
    public RunStateDto stop(UUID runId) {
        ScheduleRun run = runRepo.findById(runId).orElseThrow();
        run.setStatus(RunStatus.STOPPED);
        return toState(run);
    }

    @Override
    public void updatePosition(UUID taskId, double x, double y) {
        Task task  = taskRepo.findById(taskId).orElseThrow(()-> new RuntimeException("task was not found"));
        task.setPosX(x);
        task.setPosY(y);
        taskRepo.save(task);
    }

    @Override
    @Transactional
    public RunStateDto restart(UUID runId) {
        ScheduleRun old = runRepo.findById(runId).orElseThrow();
        old.setStatus(RunStatus.STOPPED);
        taskRepo.resetStatusByDate(old.getDate(), TaskStatus.PENDING);
        return play(old.getDate());
    }

    @Transactional(readOnly = true)
    @Override
    public RunStateDto getState(UUID runId) {
        return toState(runRepo.findById(runId).orElseThrow());
    }

    private RunStateDto toState(ScheduleRun run) {
        Task cur = run.getCurrentTask();
        Task next = (cur == null) ? null
                : linkRepo.findByFromTask(cur).map(TaskLink::getToTask).orElse(null);
        LocalTime waitingUntil = (cur == null && run.getStatus() == RunStatus.RUNNING)
                ? nextChainStart(run.getDate()).map(Task::getStartTime).orElse(null)
                : null;
        return new RunStateDto(run.getId(), run.getStatus(),
                cur == null ? null : cur.getId(),  cur == null ? null : cur.getTitle(),
                next == null ? null : next.getId(), next == null ? null : next.getTitle(),
                waitingUntil);
    }

    private Optional<Task> nextChainStart(LocalDate date) {
        return taskRepo.findChainStarts(date).stream()
                .filter(t -> t.getStatus() != TaskStatus.DONE)
                .findFirst();
    }

    @Scheduled(fixedRate = 60_000)
    @Transactional
    public void tick() {
        Instant now = Instant.now();
        LocalTime nowTime = LocalTime.now();

        for (ScheduleRun run : runRepo.findByStatus(RunStatus.RUNNING)) {
            if (run.getCurrentTask() == null) {
                // waiting between chains: start the next one when its time has come
                nextChainStart(run.getDate())
                        .filter(t -> t.getStartTime() == null || !t.getStartTime().isAfter(nowTime))
                        .ifPresent(t -> {
                            run.setCurrentTask(t);
                            run.setLastReminderAt(now);
                            events.publishEvent(new TaskStartedEvent(run.getId(), t.getId(),t.getTitle()));
                        });
                continue;
            }

            if (Duration.between(run.getLastReminderAt(), now).toMinutes() >= 30) {
                events.publishEvent(new TaskReminderEvent(run.getId(), run.getCurrentTask().getId(),run.getCurrentTask().getTitle()));
                run.setLastReminderAt(now);
            }
        }
    }
}
