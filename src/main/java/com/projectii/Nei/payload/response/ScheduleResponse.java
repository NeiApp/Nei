package com.projectii.Nei.payload.response;

import com.projectii.Nei.enums.RunStatus;
import com.projectii.Nei.model.Task;

import java.time.Instant;
import java.time.LocalDate;

public record ScheduleResponse(
        LocalDate date,
        RunStatus status,
        Task currentTask,
        Instant lastReminderAt) {
}
