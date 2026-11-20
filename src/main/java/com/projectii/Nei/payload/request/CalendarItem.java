package com.projectii.Nei.payload.request;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record CalendarItem(UUID id, String type, String title, LocalDate date,
                           LocalTime start, LocalTime end, String category,
                           String status, boolean hasAlarm) {}
