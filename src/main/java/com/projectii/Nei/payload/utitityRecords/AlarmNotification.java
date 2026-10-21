package com.projectii.Nei.payload.utitityRecords;


import com.projectii.Nei.model.Task;

import java.util.UUID;

public record AlarmNotification(UUID taskId, String title, boolean timesUp, long minutesLeft) {
    public static AlarmNotification reminder(Task t, long left) { return new AlarmNotification(t.getId(), t.getTitle(), false, left); }
    public static AlarmNotification timesUp(Task t)             { return new AlarmNotification(t.getId(), t.getTitle(), true, 0); }
    public String message() {
        return timesUp ? "Time's up: " + title : title + " is still pending (" + minutesLeft + " min left)";
    }
}