package com.projectii.Nei.payload.request;
import java.time.LocalTime;
import java.util.List;

public record CreateATask(
        String title,        // what the task is
        LocalTime startTime, // when it starts
        LocalTime endTime,   // when it ends
        AlarmRequest alarm // optional — do they want hourly reminders
) {}