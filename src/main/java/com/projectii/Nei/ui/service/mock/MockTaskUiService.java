package com.projectii.Nei.ui.service.mock;

import com.projectii.Nei.ui.model.*;
import com.projectii.Nei.ui.service.TaskUiService;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** MOCK: in-memory tasks. Replace with an implementation backed by TaskRepository, then delete this class. */
@Service
public class MockTaskUiService implements TaskUiService {
    private final List<TaskItem> tasks = new ArrayList<>();

    @PostConstruct
    void seed() {
        LocalDate d = LocalDate.now();
        tasks.add(new TaskItem("Research content ideas", "Work", d, false));
        tasks.add(new TaskItem("Create a database of guest authors", "Work", d.plusDays(2), false));
        TaskItem t = new TaskItem("Renew driver's license", "Personal", d.plusDays(3), false);
        t.subs.add(new SubItem("Book appointment", true));
        t.subs.add(new SubItem("Bring passport photo", false));
        tasks.add(t);
        tasks.add(new TaskItem("Consult accountant", "Work", d.plusDays(5), false));
        tasks.add(new TaskItem("Print business card", "Personal", d.minusDays(1), true));
        tasks.get(3).note = "Ask about VAT and quarterly filing.";
        String[] times = {"08:00 - 09:00", "09:30 - 10:30", "11:00 - 12:00", "14:00 - 15:00", "16:00 - 16:30"};
        for (int i = 0; i < tasks.size(); i++) {
            TaskItem k = tasks.get(i);
            k.time = times[i];
            k.x = 40 + i * 250;
            k.y = i % 2 == 0 ? 60 : 190;
            if (i < tasks.size() - 1) k.next = tasks.get(i + 1);
        }
    }

    /** The mock returns the live list. A real implementation returns a fresh list, so views must call save(). */
    @Override public List<TaskItem> all() { return tasks; }

    @Override public void add(TaskItem task) { tasks.add(0, task); }

    @Override public void save(TaskItem task) { /* mock: the view already changed the object */ }

    @Override public void delete(TaskItem task) {
        tasks.forEach(k -> { if (k.next == task) k.next = null; });
        tasks.remove(task);
    }

    @Override public void move(String taskId, double x, double y) {
        byId(taskId).ifPresent(t -> { t.x = x; t.y = y; });
    }

    @Override public boolean link(String fromId, String toId) {
        var a = byId(fromId);
        var b = byId(toId);
        if (a.isEmpty() || b.isEmpty()) return true;
        TaskItem from = a.get(), dest = b.get();
        for (TaskItem k = dest; k != null; k = k.next) {
            if (k == from) return false; // loop
        }
        tasks.forEach(k -> { if (k.next == dest) k.next = null; }); // one incoming thread per task
        from.next = dest;
        return true;
    }

    @Override public void unlink(String fromId, String toId) {
        byId(fromId).ifPresent(t -> { if (t.next != null && t.next.id.equals(toId)) t.next = null; });
    }

    private java.util.Optional<TaskItem> byId(String id) {
        return tasks.stream().filter(t -> t.id.equals(id)).findFirst();
    }
}
