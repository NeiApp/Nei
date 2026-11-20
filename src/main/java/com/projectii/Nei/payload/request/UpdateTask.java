package com.projectii.Nei.payload.request;

import com.projectii.Nei.enums.TaskCategory;

import java.time.LocalDate;
import java.time.LocalTime;

public record UpdateTask(String title, String description,
                         LocalDate date,//LocalDate date <-- to be used later in production,
                         LocalTime startTime, LocalTime endTime, TaskCategory category) {}