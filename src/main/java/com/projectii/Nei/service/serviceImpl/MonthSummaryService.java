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

        var alreadyExistMonthAndYear = monthSummaryRepository.existsByMonthAndYear(month, year);
        if (alreadyExistMonthAndYear) return; //returns nothing if month already exists

        List<WeeklyReview> reviews = weeklyReviewRepository.findAllByMonthAndYear(month, year);

        if (reviews.isEmpty()) return; // returns nothing if weeklyReview already empty
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
        summary.setMonth(month);
        summary.setYear(year);
        summary.setTotalTasks(totalTasks);
        summary.setCompletedTasks(completedTasks);
        summary.setSkippedTasks(skippedTasks);
        float rate = summary.getCompletionRate();
        summary.setSummaryText(buildMonthSummaryText(completedTasks, totalTasks,rate));
        return summary;
    }


    public MonthSummaryResponse getMonthSummary(int month, int year) {
        MonthSummary summary = monthSummaryRepository
                .findByMonthAndYear(month, year)
                .orElseThrow(() -> new RuntimeException(
                        "No summary found for " + month + "/" + year
                ));

        return new MonthSummaryResponse(
                summary.getId(),
                summary.getMonth(),
                summary.getYear(),
                summary.getTotalTasks(),
                summary.getCompletedTasks(),
                summary.getSkippedTasks(),
                summary.getCompletionRate(),
                summary.getSummaryText(),
                summary.getGeneratedAt()
        );
    }

    /**
     * the buildMonthSummaryText creates a detailed format of everything that was done for the last month
     *
     * @param completed the amount of tasks that where completed for that month
     * @param total     the total amount of task that was created for that month
     * @param rate  return the fix rate by dividing the completedTasks by  totalTasks and times 100
     * @return it returns a formatted string of the monthly summary
     */
    private String buildMonthSummaryText(int completed, int total, float rate) {
        return String.format(
                "You completed %d out of %d tasks this month (%.0f%%).",
                completed, total, rate
        );
    }
}
