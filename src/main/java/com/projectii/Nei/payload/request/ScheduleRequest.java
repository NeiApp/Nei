package com.projectii.Nei.payload.request;

import com.projectii.Nei.enums.RunStatus;
import com.projectii.Nei.model.Task;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;

import java.time.Instant;
import java.time.LocalDate;

public record ScheduleRequest(
        LocalDate date,
        RunStatus status,
        Task currentTask,
        Instant lastReminderAt) {
}
