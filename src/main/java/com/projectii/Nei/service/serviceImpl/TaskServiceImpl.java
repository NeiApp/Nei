package com.projectii.Nei.service.serviceImpl;

import com.projectii.Nei.enums.TaskCategory;
import com.projectii.Nei.enums.TaskStatus;
import com.projectii.Nei.mapper.TaskMapper;
import com.projectii.Nei.model.*;
import com.projectii.Nei.payload.request.*;
import com.projectii.Nei.payload.response.AlarmResponse;
import com.projectii.Nei.payload.response.CreatedTask;
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
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {

    private final TaskMapper taskMapper;

    private final WeekRepository weekRepository;

    private final WeekDayRepository weekDayRepository;

    private final TaskRepository taskRepository;

    private final NotificationSender notifier;

    private WeekService weekService;

    @Override
    @Transactional
    public CreatedTask createTask(CreateATask req) {
        if (req.title() == null || req.title().isBlank()) throw new IllegalArgumentException("title is required");
        checkTimes(req.startTime(), req.endTime());

        LocalDate date = req.date() != null ? req.date() : LocalDate.now();

        Task task = taskMapper.toEntity(req);
        task.setDate(date);
        task.setStatus(TaskStatus.PENDING);
        if (task.getCategory() == null) task.setCategory(TaskCategory.PERSONAL);
        task.setWeekDay(findWeekDay(date).orElse(null));
        if (req.alarm() != null) attachAlarm(task, req.alarm());

        return taskMapper.toTaskLinkResponse(taskRepository.save(task));
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
                .map(taskMapper::toTaskLinkResponse)
                .toList();
    }

    @Override
    public void addNoteToTask(UUID id, String note) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("task does not exist"));
        Notes addNote = new Notes();
        addNote.setNote(note);
        task.setNotes(addNote);
        taskRepository.save(task);
    }

    @Override
    @Transactional
    public AlarmResponse createAlarmReminder(UUID id, AlarmRequest req) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("task does not exist"));

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
    @Transactional
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
//                .map(subTaskMapper::toTaskLinkResponse) // optional, if you use MapStruct/manual mapper
//                .toList();

        return taskMapper.toTaskLinkResponse(task);
    }

    @Override
    @Transactional
    public CreatedTask updateTask(UUID id, UpdateTask req) {
        Task task = taskRepository.findById(id).orElseThrow(() -> new RuntimeException("task does not exist"));
        taskMapper.update(req, task);
        checkTimes(task.getStartTime(), task.getEndTime());
        if (req.date() != null) task.setWeekDay(findWeekDay(task.getDate()).orElse(null));
        if (task.getAlarm() != null) scheduleAlarm(task, task.getAlarm());
        return taskMapper.toTaskLinkResponse(task);
    }

    @Override
    @Transactional
    public void updateStatus(UUID id, StatusRequest statusRequest) {
        Task task =taskRepository.findById(id).orElseThrow(()-> new RuntimeException("Task does not exist"));
        task.setStatus(statusRequest.status());
        if (statusRequest.status() != TaskStatus.PENDING && task.getAlarm() != null) task.getAlarm().setActive(false);
    }

    private void checkTimes(LocalTime start, LocalTime end) {
        if (start != null && end != null && !end.isAfter(start)) throw new IllegalArgumentException("end time must be after start time");
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

    private static final LocalTime DEFAULT_DAY_START = LocalTime.of(8, 0);   // later: from the "Day starts" setting

    private void scheduleAlarm(Task task, Alarm alarm) {
        LocalDateTime now = LocalDateTime.now();
        LocalDate day = task.getDate();

        LocalDateTime begin = day.atTime(task.getStartTime() != null ? task.getStartTime() : DEFAULT_DAY_START);
        LocalDateTime stop  = day.atTime(task.getEndTime()   != null ? task.getEndTime()   : LocalTime.MAX);

        alarm.setStartAt(begin.isAfter(now) ? begin : now);
        alarm.setStopAt(stop);
        // first ping: at the start if it is still ahead, otherwise one interval from now
        alarm.setNextFireAt(begin.isAfter(now) ? begin : now.plusMinutes(alarm.getIntervalMinutes()));
        if (!stop.isAfter(now)) alarm.setActive(false);    // the task is already over
    }

    @Scheduled(fixedDelay = 30_000)
    @Transactional
    public void checkAlarms() {
        LocalDateTime now = LocalDateTime.now();

        for (Task task : taskRepository.findByRecurringTrueAndStatus(TaskStatus.PENDING)) {
            Alarm alarm = task.getAlarm();
            if (alarm == null || !alarm.isActive()) continue;

            // alarm window is over
            if (!now.isBefore(alarm.getStopAt())) {
                boolean fresh = Duration.between(alarm.getStopAt(), now).toMinutes() < 5;
                if (task.getEndTime() != null && fresh) notifier.send(AlarmNotification.timesUp(task));
                alarm.setActive(false);
                continue;
            }

            if (now.isBefore(alarm.getStartAt())) continue;      // not started yet (safety net)

            if (!now.isBefore(alarm.getNextFireAt())) {
                long left = Duration.between(now, alarm.getStopAt()).toMinutes();
                boolean startPing = alarm.getNextFireAt().equals(alarm.getStartAt());
                notifier.send(startPing ? AlarmNotification.start(task, left) : AlarmNotification.reminder(task, left));

                LocalDateTime next = alarm.getNextFireAt().plusMinutes(alarm.getIntervalMinutes());
                while (!next.isAfter(now)) next = next.plusMinutes(alarm.getIntervalMinutes());   // skip missed ones
                alarm.setNextFireAt(next);
            }
        }
    }

    private void attachAlarm(Task task, AlarmRequest req) {
        Alarm alarm = taskMapper.toAlarm(req);
        if (alarm.getIntervalMinutes() < 1) throw new IllegalArgumentException("interval must be at least 1 minute");
        alarm.setActive(true);
        scheduleAlarm(task, alarm);
        task.setAlarm(alarm);
        task.setRecurring(true);
    }

    private Optional<WeekDay> findWeekDay(LocalDate date) {
        return weekRepository.findByStartDateLessThanEqualAndEndDateGreaterThanEqual(date, date)
                .flatMap(week -> weekDayRepository.findByWeeksAndDayOfWeek(week, date.getDayOfWeek()));
    }
}

