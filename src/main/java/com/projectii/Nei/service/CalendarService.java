package com.projectii.Nei.service;

import com.projectii.Nei.payload.request.CalendarItem;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public interface CalendarService {
    List<CalendarItem> getRange(LocalDate from, LocalDate to);
}
