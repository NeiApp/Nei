package com.projectii.Nei.ui.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * UI view-model for a task. Public fields on purpose while this is mock data.
 * When the backend is real, map {@code model.Task} to this (or turn it into a record).
 * Fields marked NEW do not exist on the backend Task entity yet.
 */
public class TaskItem {
    public final String id = UUID.randomUUID().toString(); // backend: UUID
    public String title;
    public String description = "";   // backend: exists, but no way to edit it
    public String list;               // NEW: "Personal" | "Work"
    public String note = "";          // backend: exists
    public Integer alarm;             // backend: Alarm.intervalMinutes
    public String time = "";          // backend: startTime/endTime (UI uses one text for now)
    public double x = -1, y = -1;     // NEW: position on the Schedule canvas
    public TaskItem next;             // NEW: next task in the Schedule flow
    public LocalDate due;             // backend: Task.date
    public boolean done;              // backend: status == DONE
    public List<SubItem> subs = new ArrayList<>(); // NEW

    public TaskItem(String title, String list, LocalDate due, boolean done) {
        this.title = title;
        this.list = list;
        this.due = due;
        this.done = done;
    }
}
