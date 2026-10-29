package com.projectii.Nei.payload.utitityRecords;


import com.projectii.Nei.model.Task;

import java.util.UUID;

public record AlarmNotification(UUID taskId, String title, Kind kind, long minutesLeft) {
    public enum Kind { START, REMINDER, TIMES_UP }

    public static AlarmNotification start(Task t, long left)    { return new AlarmNotification(t.getId(), t.getTitle(), Kind.START, left); }
    public static AlarmNotification reminder(Task t, long left) { return new AlarmNotification(t.getId(), t.getTitle(), Kind.REMINDER, left); }
    public static AlarmNotification timesUp(Task t)             { return new AlarmNotification(t.getId(), t.getTitle(), Kind.TIMES_UP, 0); }

    public String message() {
        return switch (kind) {
            case START    -> "Time to start: " + title;
            case REMINDER -> title + " is still pending (" + minutesLeft + " min left)";
            case TIMES_UP -> "Time's up: " + title;
        };
    }
}