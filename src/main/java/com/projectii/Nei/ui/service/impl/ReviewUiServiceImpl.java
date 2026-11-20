package com.projectii.Nei.ui.service.impl;

import com.projectii.Nei.enums.TaskStatus;
import com.projectii.Nei.model.Task;
import com.projectii.Nei.repository.TaskRepository;
import com.projectii.Nei.ui.model.ReviewData;
import com.projectii.Nei.ui.service.ReviewUiService;
import com.projectii.Nei.ui.service.SettingsUiService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.ToIntFunction;

/**
 * The real ReviewUiService. Every number is counted from the stored tasks.
 * The sentences (summary, wins, watch, try next) are plain rules over those numbers for now;
 * the Spring AI step can replace them later without touching the screen.
 */
@Service
@Profile("!mock")
@RequiredArgsConstructor
public class ReviewUiServiceImpl implements ReviewUiService {

    private static final String NONE = "None yet";

    private final TaskRepository taskRepo;
    private final SettingsUiService settings;

    @Override
    public ReviewData review(boolean week) {
        LocalDate today = LocalDate.now();
        return week ? weekReview(today) : monthReview(today);
    }

    private ReviewData weekReview(LocalDate today) {
        DayOfWeek first = "Sunday".equals(settings.account().weekStart) ? DayOfWeek.SUNDAY : DayOfWeek.MONDAY;
        LocalDate start = today.with(TemporalAdjusters.previousOrSame(first));
        LocalDate end = start.plusDays(6);

        String[] labels = new String[7];
        for (int i = 0; i < 7; i++) {
            labels[i] = start.plusDays(i).getDayOfWeek().getDisplayName(TextStyle.NARROW, Locale.ENGLISH);
        }
        DateTimeFormatter f = DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH);
        return build("This week", f.format(start) + " - " + f.format(end), "week", start, end, labels,
                d -> (int) ChronoUnit.DAYS.between(start, d), false, today);
    }

    private ReviewData monthReview(LocalDate today) {
        LocalDate start = today.withDayOfMonth(1);
        LocalDate end = today.with(TemporalAdjusters.lastDayOfMonth());

        int weeks = (end.getDayOfMonth() + 6) / 7;
        String[] labels = new String[weeks];
        for (int i = 0; i < weeks; i++) labels[i] = "W" + (i + 1);

        return build("This month", DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH).format(today), "month",
                start, end, labels, d -> (d.getDayOfMonth() - 1) / 7, true, today);
    }

    private ReviewData build(String title, String range, String period, LocalDate start, LocalDate end,
                             String[] labels, ToIntFunction<LocalDate> bucketOf, boolean monthly, LocalDate today) {
        List<Task> tasks = taskRepo.findByDateBetweenOrderByDateAscStartTimeAsc(start, end);

        int done = 0, skipped = 0, stillOpenInPast = 0;
        int[] counts = new int[labels.length];
        Map<LocalDate, Integer> plannedPerDay = new HashMap<>();
        Map<DayOfWeek, Integer> doneByWeekday = new EnumMap<>(DayOfWeek.class);

        for (Task t : tasks) {
            plannedPerDay.merge(t.getDate(), 1, Integer::sum);
            if (t.getStatus() == TaskStatus.DONE) {
                done++;
                counts[bucketOf.applyAsInt(t.getDate())]++;
                doneByWeekday.merge(t.getDate().getDayOfWeek(), 1, Integer::sum);
            } else if (t.getStatus() == TaskStatus.SKIPPED) {
                skipped++;
            } else if (t.getDate().isBefore(today)) {
                stillOpenInPast++;
            }
        }

        int total = tasks.size();
        int pct = total == 0 ? 0 : done * 100 / total;

        DayOfWeek bestDay = null;
        int bestCount = 0;
        for (Map.Entry<DayOfWeek, Integer> e : doneByWeekday.entrySet()) {
            if (e.getValue() > bestCount) {
                bestCount = e.getValue();
                bestDay = e.getKey();
            }
        }
        String bestName = bestDay == null ? null
                : bestDay.getDisplayName(TextStyle.FULL, Locale.ENGLISH) + (monthly ? "s" : "");

        int streak = streak(today);
        int limit = settings.account().maxTasksPerDay;
        List<LocalDate> overDays = plannedPerDay.entrySet().stream()
                .filter(e -> e.getValue() > limit).map(Map.Entry::getKey).sorted().toList();

        // ---- the sentences
        String summary;
        if (total == 0) {
            summary = "Nothing was planned this " + period + " yet. Add a few tasks and they will show up here.";
        } else {
            StringBuilder sb = new StringBuilder("You finished ").append(done).append(" of ").append(total)
                    .append(" planned tasks this ").append(period).append(" (").append(pct).append("%).");
            if (bestName != null) {
                sb.append(' ').append(bestName).append(monthly ? " were your strongest days." : " was your strongest day.");
            }
            if (skipped > 0) {
                sb.append(' ').append(skipped).append(skipped == 1 ? " task was skipped." : " tasks were skipped.");
            }
            summary = sb.toString();
        }

        List<String> wins = new ArrayList<>();
        if (streak >= 2) wins.add(streak + " days in a row with at least one task finished");
        if (bestName != null) wins.add(bestName + ": " + bestCount + " finished");
        if (total >= 3 && pct >= 70) wins.add(pct + "% of planned tasks finished");

        List<String> watch = new ArrayList<>();
        if (skipped > 0) watch.add(skipped + (skipped == 1 ? " task was skipped" : " tasks were skipped"));
        if (overDays.size() == 1) {
            watch.add(overDays.get(0).format(DateTimeFormatter.ofPattern("EEEE", Locale.ENGLISH))
                    + " had more than " + limit + " tasks planned");
        } else if (overDays.size() > 1) {
            watch.add(overDays.size() + " days had more than " + limit + " tasks planned");
        }
        if (stillOpenInPast > 0) {
            watch.add(stillOpenInPast + (stillOpenInPast == 1 ? " earlier task is still open" : " earlier tasks are still open"));
        }

        String tryNext;
        if (total == 0) tryNext = "Plan two or three tasks for tomorrow to get started.";
        else if (!overDays.isEmpty()) tryNext = "Keep each day to " + limit + " tasks or fewer so more of them get finished.";
        else if (skipped > 0 || stillOpenInPast > 0) tryNext = "Move the tasks you keep skipping to a lighter day, or drop the ones you no longer need.";
        else tryNext = "Keep your " + period + " rhythm going and repeat what worked.";

        return new ReviewData(title, range, done, skipped, total, streak, bestName == null ? NONE : bestName,
                labels, counts, summary, wins, watch, tryNext);
    }

    /** Days in a row with at least one finished task, ending today (or yesterday, so mornings don't show 0). */
    private int streak(LocalDate today) {
        Set<LocalDate> doneDays = new HashSet<>();
        for (Task t : taskRepo.findByDateBetweenOrderByDateAscStartTimeAsc(today.minusDays(120), today)) {
            if (t.getStatus() == TaskStatus.DONE) doneDays.add(t.getDate());
        }
        LocalDate d = doneDays.contains(today) ? today : today.minusDays(1);
        int n = 0;
        while (doneDays.contains(d)) {
            n++;
            d = d.minusDays(1);
        }
        return n;
    }
}
