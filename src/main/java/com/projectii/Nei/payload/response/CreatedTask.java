package com.projectii.Nei.payload.response;

import com.projectii.Nei.enums.TaskCategory;
import com.projectii.Nei.model.Notes;
import com.projectii.Nei.payload.request.AlarmRequest;


import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record CreatedTask(
        UUID id,
        String title,
        LocalTime startTime,
        LocalTime endTime,
        String status,
        Notes notes,
        AlarmRequest alarm,
        LocalDate date,
        TaskCategory taskCategory,
        List<SubTaskResponse> subTask
) {}
