package com.projectii.Nei.payload.response;

import com.projectii.Nei.payload.request.AlarmRequest;


import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record CreatedTask(
        UUID id,
        String title,
        LocalTime startTime,
        LocalTime endTime,
        String status,
        String note,
        AlarmRequest alarm,
        List<SubTaskResponse> subTask
) {}
