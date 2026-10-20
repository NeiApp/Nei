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
import com.projectii.Nei.repository.TaskRepository;
import com.projectii.Nei.repository.WeekDayRepository;
import com.projectii.Nei.repository.WeekRepository;
import com.projectii.Nei.service.TaskService;
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

    private final SubTaskMapper subTaskMapper;

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
    public AlarmResponse createAlarmReminder(UUID id, AlarmRequest alarmRequest) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("task does not exist"));
        if (task.isRecurring()){
            throw new RuntimeException("task is already recurring");
        }

        Alarm alarm = taskMapper.toAlarm(alarmRequest);
        alarm.setStopAt(
                task.getDate().atTime(task.getEndTime() != null ? task.getEndTime() : LocalTime.MAX)
        );

        task.setAlarm(alarm);
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
                    // map any other fields here
                    return subTask;
                })
                .toList();

        task.addSubTasks(subTasks);

        // Because Task cascades ALL to subTasks, saving the parent persists the children.
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

    @Scheduled(fixedRate = 60000) // every 60 seconds
    public void checkAlarms() {
        var now = LocalDateTime.now();

        List<Task> tasksWithActiveAlarms = taskRepository
                .findByRecurringTrueAndStatus(TaskStatus.PENDING);

        for (Task task : tasksWithActiveAlarms) {
            Alarm alarm = task.getAlarm();
            if (alarm == null || !alarm.isActive()) continue;

            if (now.isAfter(alarm.getStopAt())) {
                alarm.setActive(false);
                continue;
            }

            long minutesSinceStart = Duration.between(alarm.getStartAt(), now).toMinutes();

            // fires every intervalMinutes, e.g. every 60 mins
            if (minutesSinceStart > 0 && minutesSinceStart % alarm.getIntervalMinutes() == 0) {
                // trigger notification — push to Vaadin UI or log for now
                System.out.println("REMINDER: " + task.getTitle() + " is still pending!");
            }
        }
    }

}

