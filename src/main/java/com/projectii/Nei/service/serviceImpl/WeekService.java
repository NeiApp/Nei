package com.projectii.Nei.service.serviceImpl;

import com.projectii.Nei.enums.TaskStatus;
import com.projectii.Nei.model.WeekDay;
import com.projectii.Nei.model.WeeklyReview;
import com.projectii.Nei.model.Weeks;
import com.projectii.Nei.payload.response.WeeklyReviewResponse;
import com.projectii.Nei.repository.WeekDayRepository;
import com.projectii.Nei.repository.WeekRepository;
import com.projectii.Nei.repository.WeeklyReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class WeekService {

    private final WeekRepository weekRepository;

    private  final WeekDayRepository weekDayRepository;

    private final WeeklyReviewRepository weeklyReviewRepository;

    @Transactional
    void startNewWeek() {
        var today = LocalDate.now();

        // Always start from Monday of the current week
        LocalDate monday = today.with(DayOfWeek.MONDAY);
        LocalDate sunday = monday.plusDays(6);

        boolean weekAlreadyExists = weekRepository
                .findByStartDateLessThanEqualAndEndDateGreaterThanEqual(today, today)
                .isPresent();
        if (weekAlreadyExists) return;

        Weeks newWeek = new Weeks();
        newWeek.setStartDate(monday);
        newWeek.setEndDate(sunday);
        weekRepository.save(newWeek);

        List<DayOfWeek> days = List.of(
                DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY,
                DayOfWeek.SATURDAY,
                DayOfWeek.SUNDAY
        );

        for (DayOfWeek day : days) {
            WeekDay weekDay = new WeekDay();
            weekDay.setWeeks(newWeek);
            weekDay.setDayOfWeek(day);
            weekDay.setDate(monday.plusDays(day.getValue() - 1));
            weekDayRepository.save(weekDay);
        }
    }

    // On app startup — catch up on any missed weeks
   public void initializeFirstWeek() {
       var today = LocalDate.now();

       boolean weekExistsForToday = weekRepository
               .findByStartDateLessThanEqualAndEndDateGreaterThanEqual(today, today)
               .isPresent();

       if (!weekExistsForToday) {
           startNewWeek();
       }
   }

    // TODO(roadmap #5): runs every minute "for testing" and reviews the CURRENT week.

    // Run it when a week ends, review the week that just ended, and set WeekStatus.COMPLETED.

    @Scheduled(cron = "0 * * * * *") // fires every minute, for testing only
    @Transactional
    public void generateWeeklyReview() {
        LocalDate today = LocalDate.now();

        Weeks week = weekRepository
                .findByStartDateLessThanEqualAndEndDateGreaterThanEqual(today, today)
                .orElseThrow(() -> new RuntimeException("No active week found"));

        if (week.getWeeklyReview() != null) return;

        List<WeekDay> days = week.getDays();

        int totalTasks = 0;
        int completedTasks = 0;
        int skippedTasks = 0;
        int streakDays = 0;
        String bestDay = null;
        int bestDayCount = 0;

        for (WeekDay day : days) {
            boolean wasInactive = day.getDayLog() != null
                    && day.getDayLog().getReflection() == null
                    && day.getTasks().isEmpty();

            if (wasInactive) {
                streakDays = 0;
                continue;
            }

            int dayTotal = day.getTotalTasks();
            int dayCompleted = day.getCompletedTasks();
            int daySkipped = (int) day.getTasks().stream()
                    .filter(t -> t.getStatus() == TaskStatus.SKIPPED)
                    .count();

            totalTasks += dayTotal;
            completedTasks += dayCompleted;
            skippedTasks += daySkipped;

            if (dayTotal > 0 && dayCompleted == dayTotal) {
                streakDays++;
            } else {
                streakDays = 0;
            }

            if (dayCompleted > bestDayCount) {
                bestDayCount = dayCompleted;
                bestDay = day.getDayOfWeek().name();
            }
        }

        String summaryText = buildSummary(completedTasks, totalTasks, streakDays, bestDay);

        WeeklyReview weeklyReview = new WeeklyReview();
        weeklyReview.setWeeks(week);
        weeklyReview.setTotalTasks(totalTasks);
        weeklyReview.setCompletedTasks(completedTasks);
        weeklyReview.setSkippedTasks(skippedTasks);
        weeklyReview.setStreakDays(streakDays);
        weeklyReview.setBestDay(bestDay);
        weeklyReview.setSummaryText(summaryText);

        WeeklyReview saved = weeklyReviewRepository.save(weeklyReview);

    }

    public WeeklyReviewResponse getWeeklyReview(UUID weekId) {
        Weeks week = weekRepository.findById(weekId)
                .orElseThrow(() -> new RuntimeException("week was not found"));

        WeeklyReview review = week.getWeeklyReview();

        if (review == null) {
            throw new RuntimeException("Review not generated yet — week may still be active");
        }

        return new WeeklyReviewResponse(
                review.getId(),
                review.getTotalTasks(),
                review.getCompletedTasks(),
                review.getSkippedTasks(),
                review.getStreakDays(),
                review.getBestDay(),
                review.getSummaryText(),
                review.getGeneratedAt()
        );
    }

    private String buildSummary(int completed, int total, int streak, String bestDay) {
        float rate = total > 0 ? (float) completed / total * 100 : 0f;
        return String.format(
                "You completed %d out of %d tasks this week (%.0f%%). Best day: %s. Streak: %d days.",
                completed, total, rate,
                bestDay != null ? bestDay : "none",
                streak
        );
    }



}
