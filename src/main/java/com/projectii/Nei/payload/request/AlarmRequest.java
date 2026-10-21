package com.projectii.Nei.payload.request;

import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;

public record AlarmRequest(
        @Positive int intervalMinutes,
        LocalDateTime startAt,
        boolean active,
        boolean recurring
) {}
