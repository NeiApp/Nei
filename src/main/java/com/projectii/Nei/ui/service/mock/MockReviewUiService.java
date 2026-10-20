package com.projectii.Nei.ui.service.mock;

import com.projectii.Nei.ui.model.ReviewData;
import com.projectii.Nei.ui.service.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** MOCK: fixed review text. The real one reads a saved review that Spring AI generated when the period closed. */
@Service
public class MockReviewUiService implements ReviewUiService {
    private final SettingsUiService settings;

    public MockReviewUiService(SettingsUiService settings) {
        this.settings = settings;
    }

    @Override
    public ReviewData review(boolean week) {
        LocalDate now = LocalDate.now();
        if (week) {
            LocalDate mon = now.with(java.time.temporal.TemporalAdjusters.previousOrSame(
                    settings.account().weekStart.equals("Sunday") ? java.time.DayOfWeek.SUNDAY : java.time.DayOfWeek.MONDAY));
            DateTimeFormatter f = DateTimeFormatter.ofPattern("MMM d");
            return new ReviewData("This week", mon.format(f) + " - " + mon.plusDays(6).format(f), 18, 3, 21, 5, "Wednesday",
                    new String[]{"M", "T", "W", "T", "F", "S", "S"}, new int[]{3, 2, 5, 3, 3, 2, 0},
                    "A steady week. You kept a five-day streak and Wednesday was your strongest day, "
                            + "when you finished everything you planned. The three skipped tasks were all after 6pm.",
                    List.of("Five days in a row with at least two tasks done", "Wednesday: 5 of 5 finished"),
                    List.of("Evening tasks were skipped three times", "Saturday ran longer than planned"),
                    "Move one evening task into the morning and protect a quiet hour on Wednesday again.");
        }
        return new ReviewData("This month", now.format(DateTimeFormatter.ofPattern("MMMM yyyy")), 41, 7, 48, 12, "Wednesdays",
                new String[]{"W1", "W2", "W3", "W4"}, new int[]{9, 12, 11, 9},
                "You finished most of what you planned. Mornings were your strongest time, and the tasks you "
                        + "skipped were mostly in the evening. Week two was your best.",
                List.of("12-day streak, your longest yet", "85% of planned tasks completed"),
                List.of("Skipped tasks cluster after 6pm", "Week four dipped"),
                "Plan fewer evening tasks next month and keep your mornings for the big ones.");
    }
}
