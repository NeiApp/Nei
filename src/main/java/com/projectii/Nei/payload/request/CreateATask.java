package com.projectii.Nei.payload.request;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

public record CreateATask(
        @NotBlank String title,
        String description,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime,
        @Valid AlarmRequest alarm // optional
) {}