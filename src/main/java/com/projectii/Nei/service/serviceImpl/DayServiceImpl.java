package com.projectii.Nei.service.serviceImpl;

import org.springframework.transaction.annotation.Transactional;
import com.projectii.Nei.model.DayLog;
import com.projectii.Nei.model.WeekDay;
import com.projectii.Nei.model.Weeks;
import com.projectii.Nei.payload.response.DayLogResponse;
import com.projectii.Nei.repository.DayLogRepository;
import com.projectii.Nei.repository.WeekDayRepository;
import com.projectii.Nei.repository.WeekRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DayServiceImpl {
    private final DayLogRepository dayLogRepository;
    private final WeekDayRepository weekDayRepository;
    private final WeekRepository weekRepository;

    @Scheduled(cron = "0 0 21 * * *")
    @Transactional
    public void triggerEndOfTheDay(){
        var today = LocalDate.now();

       Weeks currentWeek =  weekRepository
                .findByStartDateLessThanEqualAndEndDateGreaterThanEqual(today, today)
                .orElseThrow(() -> new RuntimeException("No active week found"));

        WeekDay todayWeekDay = currentWeek.getDays().stream()
                .filter(d -> d.getDate().equals(today))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No WeekDay found for today"));

        // Don't create duplicate if it already exists
        if (todayWeekDay.getDayLog() != null) return;

        DayLog dayLog = new DayLog();
        dayLog.setWeekDay(todayWeekDay);
        dayLog.setLoggedAt(LocalDateTime.now());

        DayLog saved = dayLogRepository.save(dayLog);

        todayWeekDay.setDayLog(saved);
        weekDayRepository.save(todayWeekDay);


        log.info("Rei How was your day? Tap to reflect.");

    }

    public void submitReflection(UUID id, String reflection) {
      DayLog  dayLog =   dayLogRepository.findById(id).orElseThrow(() -> new RuntimeException("Daily logs reflection was not found"));
      dayLog.setReflection(reflection);

      dayLogRepository.save(dayLog);
    }

    @Transactional(readOnly = true)

    public DayLogResponse getTodayDayLog() {
        var today = LocalDate.now();

        Weeks currentWeek = weekRepository.findByStartDateLessThanEqualAndEndDateGreaterThanEqual(today,today)
                .orElseThrow(() -> new RuntimeException("No active week found"));

        WeekDay todayWeekDay = currentWeek.getDays()
                .stream().filter(d -> d.getDate().equals(today))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No active week day found"));

        DayLog dayLog = todayWeekDay.getDayLog();

        if (dayLog == null){
            return null;
        }

        return new DayLogResponse(dayLog.getId(),dayLog.getReflection(),dayLog.getLoggedAt());
    }
}
