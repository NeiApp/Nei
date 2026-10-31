package com.projectii.Nei.payload.response;


import com.projectii.Nei.enums.RunStatus;

import java.time.LocalTime;
import java.util.UUID;

public record RunStateDto(
        UUID runId, RunStatus status,
                          UUID currentTaskId, String currentTaskTitle,
                          UUID nextTaskId, String nextTaskTitle,
                          LocalTime waitingUntil) {}
