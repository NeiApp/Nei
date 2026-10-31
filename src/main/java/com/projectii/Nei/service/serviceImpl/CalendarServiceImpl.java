package com.projectii.Nei.service.serviceImpl;

import com.projectii.Nei.payload.request.CalendarItem;
import com.projectii.Nei.repository.TaskRepository;
import com.projectii.Nei.service.CalendarService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;


@Service
@RequiredArgsConstructor
public class CalendarServiceImpl implements CalendarService {
    private final TaskRepository taskRepository;

    @Transactional(readOnly = true)
    public List<CalendarItem> getRange(LocalDate from, LocalDate to) {
        if (to.isBefore(from)) throw new IllegalArgumentException("'to' must not be before 'from'");
        if (ChronoUnit.DAYS.between(from, to) > 62) throw new IllegalArgumentException("range is too long");

        return taskRepository.findByDateBetweenOrderByDateAscStartTimeAsc(from, to).stream()
                .map(t -> new CalendarItem(t.getId(), "TASK", t.getTitle(), t.getDate(),
                        t.getStartTime(), t.getEndTime(), t.getCategory().name(), t.getStatus().name(),
                        t.getAlarm() != null && t.getAlarm().isActive()))
                .toList();
    }
}
