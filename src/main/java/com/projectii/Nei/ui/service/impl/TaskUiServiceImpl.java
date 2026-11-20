package com.projectii.Nei.ui.service.impl;

import com.projectii.Nei.enums.TaskCategory;
import com.projectii.Nei.enums.TaskStatus;
import com.projectii.Nei.model.Notes;
import com.projectii.Nei.model.SubTasks;
import com.projectii.Nei.model.Task;
import com.projectii.Nei.model.TaskLink;
import com.projectii.Nei.payload.request.AlarmRequest;
import com.projectii.Nei.payload.request.StatusRequest;
import com.projectii.Nei.repository.TaskLinkRepository;
import com.projectii.Nei.repository.TaskRepository;
import com.projectii.Nei.service.SchedulerService;
import com.projectii.Nei.service.TaskLinkService;
import com.projectii.Nei.service.TaskService;
import com.projectii.Nei.service.serviceImpl.WeekService;
import com.projectii.Nei.ui.model.SubItem;
import com.projectii.Nei.ui.model.TaskItem;
import com.projectii.Nei.ui.service.TaskUiService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.notification.Notification;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The real TaskUiService: the screens talk to the backend through this class.
 * <p>
 * The views compare tasks with {@code ==} (selected, running, next) and change a TaskItem before calling save(),
 * so this service keeps ONE TaskItem per task and refreshes those same objects from the database. After every
 * change it reloads, so what the screen shows is always what was stored (for example when a reminder could not be set).
 * <p>
 * Run with {@code --spring.profiles.active=mock} to use {@code MockTaskUiService} instead.
 */
@Service
@Profile("!mock")
public class TaskUiServiceImpl implements TaskUiService {

    /** Many cells ask for all() while one screen is drawn; they share one snapshot this old. */
    private static final long SNAPSHOT_MS = 1_000;
    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");
    /** "09:00 - 10:00" or just "09:00". */
    private static final Pattern TIME_TEXT =
            Pattern.compile("^\\s*(\\d{1,2}):(\\d{2})\\s*(?:[-\\u2013\\u2014]\\s*(\\d{1,2}):(\\d{2}))?\\s*$");

    private final TaskRepository taskRepo;
    private final TaskLinkRepository linkRepo;
    private final TaskService taskService;
    private final TaskLinkService linkService;
    private final SchedulerService scheduler;
    private final WeekService weekService;
    private final TransactionTemplate tx;
    private final TransactionTemplate readTx;

    private final Map<String, TaskItem> cache = new LinkedHashMap<>();
    private long loadedAt = 0;

    public TaskUiServiceImpl(TaskRepository taskRepo, TaskLinkRepository linkRepo, TaskService taskService,
                             TaskLinkService linkService, SchedulerService scheduler, WeekService weekService,
                             PlatformTransactionManager txManager) {
        this.taskRepo = taskRepo;
        this.linkRepo = linkRepo;
        this.taskService = taskService;
        this.linkService = linkService;
        this.scheduler = scheduler;
        this.weekService = weekService;
        this.tx = new TransactionTemplate(txManager);
        this.readTx = new TransactionTemplate(txManager);
        this.readTx.setReadOnly(true);
    }

    // ------------------------------------------------------------------ TaskUiService

    @Override
    public synchronized List<TaskItem> all() {
        if (System.currentTimeMillis() - loadedAt > SNAPSHOT_MS) reload();
        return new ArrayList<>(cache.values());
    }

    @Override
    public synchronized void add(TaskItem t) {
        Times times = parseTimes(t.time);
        LocalDate date = t.due != null ? t.due : LocalDate.now();

        UUID id = tx.execute(status -> {
            Task e = new Task();
            e.setStatus(TaskStatus.PENDING);
            fill(e, t, times, date);
            e.setWeekDay(weekService.findWeekDay(date).orElse(null));
            return taskRepo.save(e).getId();
        });

        t.id = id.toString();   // the view keeps using this very object, so give it the real id
        cache.put(t.id, t);
        if (t.alarm != null && !t.done) setAlarm(id, t.alarm);
        if (t.done) taskService.updateStatus(id, new StatusRequest(TaskStatus.DONE));
        reload();
    }

    @Override
    public synchronized void save(TaskItem t) {
        UUID id = UUID.fromString(t.id);
        Times times = parseTimes(t.time);
        LocalDate date = t.due != null ? t.due : LocalDate.now();

        Before before = tx.execute(status -> {
            Task e = taskRepo.findById(id)
                    .orElseThrow(() -> new IllegalStateException("That task no longer exists"));
            Before old = new Before(e.getAlarm() == null ? null : e.getAlarm().getIntervalMinutes(),
                    e.getDate(), e.getStartTime(), e.getEndTime(), e.getStatus() == TaskStatus.DONE);
            fill(e, t, times, date);
            if (!Objects.equals(old.date(), date)) e.setWeekDay(weekService.findWeekDay(date).orElse(null));
            taskRepo.save(e);
            return old;
        });

        // the reminder window depends on the date and times, so a moved task needs its reminder re-planned
        boolean moved = !Objects.equals(before.date(), date)
                || !Objects.equals(before.start(), times.start())
                || !Objects.equals(before.end(), times.end());
        boolean alarmChanged = !Objects.equals(before.alarm(), t.alarm);
        if (alarmChanged || (t.alarm != null && moved)) {
            if (t.alarm == null || !t.done) setAlarm(id, t.alarm);
        }
        if (t.done != before.done()) {
            taskService.updateStatus(id, new StatusRequest(t.done ? TaskStatus.DONE : TaskStatus.PENDING));
        }
        reload();
    }

    @Override
    public synchronized void delete(TaskItem t) {
        taskService.deleteTask(UUID.fromString(t.id));   // also removes threads and schedule runs that point at it
        cache.remove(t.id);
        reload();
    }

    @Override
    public synchronized void move(String taskId, double x, double y) {
        scheduler.updatePosition(UUID.fromString(taskId), x, y);
        TaskItem item = cache.get(taskId);
        if (item != null) {
            item.x = x;
            item.y = y;
        }
    }

    @Override
    public synchronized boolean link(String fromId, String toId) {
        UUID from = UUID.fromString(fromId);
        UUID to = UUID.fromString(toId);
        if (linkService.wouldLoop(from, to)) return false;
        linkService.relink(from, to);   // one thread out and one thread in per task, like the mock
        reload();
        return true;
    }

    @Override
    public synchronized void unlink(String fromId, String toId) {
        linkService.deleteLinkBetween(UUID.fromString(fromId), UUID.fromString(toId));
        reload();
    }

    /** Something outside this service changed tasks: read them again on the next all(). */
    @EventListener
    public synchronized void onTasksChanged(TasksChangedEvent event) {
        loadedAt = 0;
    }

    // ------------------------------------------------------------------ database -> screen

    private void reload() {
        readTx.executeWithoutResult(status -> {
            List<Task> rows = new ArrayList<>(taskRepo.findAllWithSubTasks());
            rows.sort(TaskUiServiceImpl::byDateThenTime);

            Map<UUID, UUID> nextOf = new HashMap<>();
            for (TaskLink l : linkRepo.findAll()) nextOf.put(l.getFromTask().getId(), l.getToTask().getId());

            Map<String, TaskItem> fresh = new LinkedHashMap<>();
            for (Task t : rows) {
                String id = t.getId().toString();
                TaskItem item = cache.get(id);                 // same object as before, so == keeps working
                if (item == null) item = new TaskItem(t.getTitle(), "Personal", t.getDate(), false);
                item.id = id;
                copy(t, item);
                fresh.put(id, item);
            }
            for (Task t : rows) {
                UUID next = nextOf.get(t.getId());
                fresh.get(t.getId().toString()).next = next == null ? null : fresh.get(next.toString());
            }

            cache.clear();
            cache.putAll(fresh);
            loadedAt = System.currentTimeMillis();
        });
    }

    private static void copy(Task t, TaskItem item) {
        item.title = t.getTitle();
        item.description = nz(t.getDescription());
        item.list = t.getCategory() == TaskCategory.WORK ? "Work" : "Personal";
        item.note = t.getNotes() == null ? "" : nz(t.getNotes().getNote());
        item.alarm = t.getAlarm() == null ? null : t.getAlarm().getIntervalMinutes();
        item.time = formatTime(t.getStartTime(), t.getEndTime());
        item.x = t.getPosX() == null ? -1 : t.getPosX();
        item.y = t.getPosY() == null ? -1 : t.getPosY();
        item.due = t.getDate();
        item.done = t.getStatus() == TaskStatus.DONE;
        item.subs.clear();
        for (SubTasks s : t.getSubTasks()) item.subs.add(new SubItem(nz(s.getNameOfSubTask()), s.isDone()));
    }

    // ------------------------------------------------------------------ screen -> database

    /** Copies everything the screen can edit onto the entity (status and alarm are handled separately). */
    private static void fill(Task e, TaskItem t, Times times, LocalDate date) {
        e.setTitle(t.title == null ? "" : t.title.trim());
        e.setDescription(nz(t.description));
        e.setCategory("Work".equals(t.list) ? TaskCategory.WORK : TaskCategory.PERSONAL);
        e.setDate(date);
        e.setStartTime(times.start());
        e.setEndTime(times.end());

        String note = nz(t.note);
        if (note.isBlank()) {
            e.setNotes(null);                       // orphan removal deletes the old note row
        } else {
            Notes n = e.getNotes() != null ? e.getNotes() : new Notes();
            n.setNote(note);
            e.setNotes(n);
        }

        e.setPosX(t.x < 0 ? null : t.x);
        e.setPosY(t.y < 0 ? null : t.y);

        e.getSubTasks().clear();                    // replace the list; orphan removal cleans up the old rows
        for (SubItem s : t.subs) {
            SubTasks st = new SubTasks();
            st.setNameOfSubTask(s.title);
            st.setDone(s.done);
            e.addSubTask(st);
        }
    }

    /** Sets or removes the repeating reminder. A reminder the backend refuses is reported, not thrown at the screen. */
    private void setAlarm(UUID id, Integer minutes) {
        try {
            if (minutes == null) {
                tx.executeWithoutResult(status -> taskRepo.findById(id).ifPresent(e -> {
                    e.setAlarm(null);               // orphan removal deletes the alarm row
                    e.setRecurring(false);
                    taskRepo.save(e);
                }));
            } else {
                taskService.createAlarmReminder(id, new AlarmRequest(minutes, null, null));
            }
        } catch (RuntimeException ex) {
            tell("Reminder not set: " + ex.getMessage());
        }
    }

    // ------------------------------------------------------------------ small helpers

    private record Times(LocalTime start, LocalTime end) {}

    private record Before(Integer alarm, LocalDate date, LocalTime start, LocalTime end, boolean done) {}

    /** "09:00 - 10:00" -> times. Blank means no time. Anything else is reported and left empty. */
    private static Times parseTimes(String text) {
        if (text == null || text.isBlank()) return new Times(null, null);
        Matcher m = TIME_TEXT.matcher(text);
        if (m.matches()) {
            try {
                LocalTime start = LocalTime.of(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)));
                LocalTime end = m.group(3) == null ? null
                        : LocalTime.of(Integer.parseInt(m.group(3)), Integer.parseInt(m.group(4)));
                if (end != null && !end.isAfter(start)) {
                    tell("The end time has to be after the start time, so only the start was saved.");
                    end = null;
                }
                return new Times(start, end);
            } catch (DateTimeException ignored) {
                // fall through to the message below
            }
        }
        tell("I couldn't read that time. Try something like 09:00 - 10:00.");
        return new Times(null, null);
    }

    private static String formatTime(LocalTime start, LocalTime end) {
        if (start == null) return "";
        return end == null ? start.format(HH_MM) : start.format(HH_MM) + " - " + end.format(HH_MM);
    }

    private static int byDateThenTime(Task a, Task b) {
        int c = nullsLast(a.getDate(), b.getDate());
        if (c == 0) c = nullsLast(a.getStartTime(), b.getStartTime());
        if (c == 0) c = nullsLast(a.getCreatedAt(), b.getCreatedAt());
        return c;
    }

    private static <T extends Comparable<? super T>> int nullsLast(T a, T b) {
        if (a == null) return b == null ? 0 : 1;
        if (b == null) return -1;
        return a.compareTo(b);
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    private static void tell(String message) {
        if (UI.getCurrent() != null) Notification.show(message);
    }
}
