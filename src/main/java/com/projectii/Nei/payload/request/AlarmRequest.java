package com.projectii.Nei.payload.request;

import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;
import java.time.LocalTime;

public record AlarmRequest(
        @Positive int intervalMinutes,
        LocalTime startTime,    // optional, e.g. 15:00
        LocalTime endTime       // optional, e.g. 17:00
) {}
