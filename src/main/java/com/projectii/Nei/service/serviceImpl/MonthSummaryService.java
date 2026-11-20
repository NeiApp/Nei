package com.projectii.Nei.service.serviceImpl;

import com.projectii.Nei.model.MonthSummary;
import com.projectii.Nei.model.WeeklyReview;
import com.projectii.Nei.payload.response.MonthSummaryResponse;
import com.projectii.Nei.repository.MonthSummaryRepository;
import com.projectii.Nei.repository.WeeklyReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MonthSummaryService {
    private final WeeklyReviewRepository weeklyReviewRepository;
    private final MonthSummaryRepository monthSummaryRepository;

    /**
     * every 00.05 the monthly summary method is triggered and created
     */
    @Scheduled(cron = "0 5 0 1 * *")
    void generateMonthlySummary (){
        LocalDate date = LocalDate.now();
        LocalDate previousDate = date.minusMonths(1);
        var month = previousDate.getMonthValue();
        var year = previousDate.getYear();

        // FIX: Use new method name existsBySummaryMonthAndSummaryYear
        var alreadyExistMonthAndYear = monthSummaryRepository.existsBySummaryMonthAndSummaryYear(month, year);
        if (alreadyExistMonthAndYear) return;

        // NOTE: Ensure WeeklyReviewRepository also has a method matching its own fields.
        // If WeeklyReview uses 'month'/'year', this line might still fail if that repo isn't updated too.
        // Assuming WeeklyReviewRepo is correct for now:
        List<WeeklyReview> reviews = weeklyReviewRepository.findAllByMonthAndYear(month, year);

        if (reviews.isEmpty()) return;

        MonthSummary summary = getMonthSummary(reviews, month, year);

        monthSummaryRepository.save(summary);
    }

    private MonthSummary getMonthSummary(List<WeeklyReview> reviews, int month, int year) {
        int totalTasks = 0;
        int completedTasks = 0;
        int skippedTasks = 0;

        for (WeeklyReview review : reviews) {
            totalTasks += review.getTotalTasks();
            completedTasks += review.getCompletedTasks();
            skippedTasks += review.getSkippedTasks();
        }

        MonthSummary summary = new MonthSummary();

        // FIX: Use new setter names setSummaryMonth / setSummaryYear
        summary.setSummaryMonth(month);
        summary.setSummaryYear(year);

        summary.setTotalTasks(totalTasks);
        summary.setCompletedTasks(completedTasks);
        summary.setSkippedTasks(skippedTasks);

        float rate = summary.getCompletionRate();
        summary.setSummaryText(buildMonthSummaryText(completedTasks, totalTasks, rate));
        return summary;
    }


    public MonthSummaryResponse getMonthSummary(int month, int year) {
        // FIX: Use new finder method findBySummaryMonthAndSummaryYear
        MonthSummary summary = monthSummaryRepository
                .findBySummaryMonthAndSummaryYear(month, year)
                .orElseThrow(() -> new RuntimeException(
                        "No summary found for " + month + "/" + year
                ));

        return new MonthSummaryResponse(
                summary.getId(),
                // FIX: Use new getter names getSummaryMonth / getSummaryYear
                summary.getSummaryMonth(),
                summary.getSummaryYear(),
                summary.getTotalTasks(),
                summary.getCompletedTasks(),
                summary.getSkippedTasks(),
                summary.getCompletionRate(),
                summary.getSummaryText(),
                summary.getGeneratedAt()
        );
    }

    private String buildMonthSummaryText(int completed, int total, float rate) {
        return String.format(
                "You completed %d out of %d tasks this month (%.0f%%).",
                completed, total, rate
        );
    }
}