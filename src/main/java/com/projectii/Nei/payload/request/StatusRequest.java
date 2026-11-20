package com.projectii.Nei.payload.request;

import com.projectii.Nei.enums.TaskStatus;

public record StatusRequest(TaskStatus status) {}