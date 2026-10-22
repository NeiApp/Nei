package com.projectii.Nei.service.serviceImpl;

import com.projectii.Nei.enums.TaskStatus;
import com.projectii.Nei.exception.ActiveWeekException;
import com.projectii.Nei.mapper.SubTaskMapper;
import com.projectii.Nei.mapper.TaskMapper;
import com.projectii.Nei.model.*;
import com.projectii.Nei.payload.request.AlarmRequest;
import com.projectii.Nei.payload.request.CreateATask;
import com.projectii.Nei.payload.request.SubTaskRequest;
import com.projectii.Nei.payload.response.AlarmResponse;
import com.projectii.Nei.payload.response.CreatedTask;
import com.projectii.Nei.payload.response.SubTaskResponse;
import com.projectii.Nei.payload.utitityRecords.AlarmNotification;
import com.projectii.Nei.repository.TaskRepository;
import com.projectii.Nei.repository.WeekDayRepository;
import com.projectii.Nei.repository.WeekRepository;
import com.projectii.Nei.service.TaskService;
import com.projectii.Nei.service.utilityService.NotificationSender;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {

    private final TaskMapper taskMapper;

    private final WeekRepository weekRepository;

    private final WeekDayRepository weekDayRepository;

    private final TaskRepository taskRepository;

    private final NotificationSender notifier;

    // TODO(roadmap #7): always uses today. Add date, list and description to CreateATask.

    @Override

    @Transactional

    public CreatedTask createTask(CreateATask createATask) {
        var today = LocalDate.now();

        // Find current week by date
        Weeks week = weekRepository
                .findByStartDateLessThanEqualAndEndDateGreaterThanEqual(today, today)
                .orElseThrow(() -> new ActiveWeekException("No active week found"));

        // Find today's WeekDay inside that week
        DayOfWeek dayOfWeek = today.getDayOfWeek();

        WeekDay weekDay = weekDayRepository
                .findByWeeksAndDayOfWeek(week, dayOfWeek)
                .orElseThrow(() -> new ActiveWeekException("No WeekDay found for today"));

        // Map and save
        Task task = taskMapper.toEntity(createATask);
        task.setWeekDay(weekDay);
        task.setDate(today);
        task.setStatus(TaskStatus.PENDING);
        task.getAlarm().setActive(false);

        return taskMapper.toResponse(taskRepository.save(task));
    }

    @Override
    public void completeATaskHandler(UUID id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("task does not exist"));

        if (!task.getStatus().equals(TaskStatus.PENDING)) {
            throw new RuntimeException("task is already " + task.getStatus());
        }

        task.setStatus(TaskStatus.DONE);
        taskRepository.save(task);
    }

    @Override
    public List<CreatedTask> getTodayTasks() {
        LocalDate today = LocalDate.now();
        List<Task> tasks = taskRepository.findByDate(today);
        return tasks.stream()
                .map(taskMapper::toResponse)
                .toList();
    }

    @Override
    public void addNoteToTask(UUID id, String note) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("task does not exist"));

        task.setNote(note);
        taskRepository.save(task);
    }

    @Override
    @Transactional
    public AlarmResponse createAlarmReminder(UUID id, AlarmRequest req) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("task does not exist"));

        if (req.intervalMinutes() < 1)
            throw new RuntimeException("interval must be at least 1 minute");

        LocalDate date = task.getDate();

        // alarm window: request first, then task times, then sensible defaults
        LocalTime startTime = req.startTime() != null ? req.startTime()
                : task.getStartTime() != null ? task.getStartTime()
                : LocalTime.now();
        LocalTime endTime = req.endTime() != null ? req.endTime()
                : task.getEndTime() != null ? task.getEndTime()
                : LocalTime.MAX;

        LocalDateTime startAt = date.atTime(startTime);
        LocalDateTime stopAt  = date.atTime(endTime);

        if (!stopAt.isAfter(startAt))
            throw new RuntimeException("alarm end must be after alarm start");
        if (!stopAt.isAfter(LocalDateTime.now()))
            throw new RuntimeException("the alarm window has already ended");

        Alarm alarm = new Alarm();
        alarm.setIntervalMinutes(req.intervalMinutes());
        alarm.setStartAt(startAt);
        alarm.setStopAt(stopAt);
        alarm.setNextFireAt(startAt);   // first reminder at the start time
        alarm.setActive(true);

        task.setAlarm(alarm);           // replaces any existing alarm
        task.setRecurring(true);
        taskRepository.save(task);
        return new AlarmResponse(alarm.isActive());
    }


    @Override
    public void deleteTask(UUID id) {
        taskRepository.deleteById(id);
    }

    @Override
    public CreatedTask addSubTasks(UUID taskId, List<SubTaskRequest> requests) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Task not found with id: " + taskId));

        List<SubTasks> subTasks = requests.stream()
                .map(request -> {
                    SubTasks subTask = new SubTasks();
                    subTask.setNameOfSubTask(request.nameOfSubTask());
                    return subTask;
                })
                .toList();

        task.addSubTasks(subTasks);

        taskRepository.save(task);

//        return subTasks.stream()
//                .map(subTaskMapper::toResponse) // optional, if you use MapStruct/manual mapper
//                .toList();

        return taskMapper.toResponse(task);
    }

    @Scheduled(cron = "0 59 23 * * *") // fires 23:59 every day
    public void autoSkipUnfinishedTasks() {
        var today = LocalDate.now();

        List<Task> pendingTasks = taskRepository
                .findByDateAndStatus(today, TaskStatus.PENDING);

        pendingTasks.forEach(task -> {
            task.setStatus(TaskStatus.SKIPPED);
            taskRepository.save(task);
        });
    }

    // TODO(roadmap #6): only prints. Store nextFireAt on Alarm, fire when now >= nextFireAt, save, and

    // send through a NotificationSender interface (desktop tray now, phone later).

    @Scheduled(fixedDelay = 30_000)
    @Transactional
    public void checkAlarms() {
        LocalDateTime now = LocalDateTime.now();

        for (Task task : taskRepository.findByRecurringTrueAndStatus(TaskStatus.PENDING)) {
            Alarm alarm = task.getAlarm();
            if (alarm == null || !alarm.isActive()) continue;

            // alarm window is over
            if (!now.isBefore(alarm.getStopAt())) {
                notifier.send(AlarmNotification.timesUp(task));
                alarm.setActive(false);
                continue;
            }

            // not started yet (nextFireAt = startAt, so this is a safety net)
            if (now.isBefore(alarm.getStartAt())) continue;

            // interval reminder
            if (!now.isBefore(alarm.getNextFireAt())) {
                long left = Duration.between(now, alarm.getStopAt()).toMinutes();
                notifier.send(AlarmNotification.reminder(task, left));

                LocalDateTime next = alarm.getNextFireAt().plusMinutes(alarm.getIntervalMinutes());
                while (!next.isAfter(now)) next = next.plusMinutes(alarm.getIntervalMinutes()); // skip missed ones
                alarm.setNextFireAt(next);
            }
        }
    }
}

