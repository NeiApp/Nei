package com.projectii.Nei.payload.request;
import com.projectii.Nei.enums.TaskCategory;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

public record CreateATask(
        @NotBlank String title,
        String description,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime,
        TaskCategory taskCategory,
        LocalDate date,
        @Valid AlarmRequest alarm // optional
) {}