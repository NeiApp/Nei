package com.projectii.Nei.ui.service;

import com.projectii.Nei.ui.model.TaskItem;
import java.util.List;

/**
 * What the Today, Calendar and Schedule screens need from the backend.
 * Each method says what exists today. Implement this against TaskRepository and delete the mock.
 */
public interface TaskUiService {
    /** Backend today: only today's tasks (TaskService.getTodayTasks). Needs a date-range query. */
    List<TaskItem> all();

    /** Backend today: createTask(CreateATask), but it ignores date, list and description. */
    void add(TaskItem task);

    /** Backend today: nothing. Needs an update endpoint (title, description, time, list, due date, done, note, alarm). */
    void save(TaskItem task);

    /** Backend today: deleteTask(UUID). Must also clear any "next" link that points at the deleted task. */
    void delete(TaskItem task);

    /** Backend today: nothing. Needs positionX and positionY on Task. */
    void move(String taskId, double x, double y);

    /** Backend today: nothing. Needs nextTaskId on Task. @return false if the link would make a loop. */
    boolean link(String fromId, String toId);

    /** Backend today: nothing. Clears the "next" link from one task. */
    void unlink(String fromId, String toId);
}
