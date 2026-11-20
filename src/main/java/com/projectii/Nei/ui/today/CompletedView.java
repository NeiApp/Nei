package com.projectii.Nei.ui.today;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.projectii.Nei.ui.model.*;
import com.projectii.Nei.ui.service.*;
import com.projectii.Nei.ui.shared.*;
import com.projectii.Nei.ui.app.MainLayout;

@CssImport("./styles/completed.css")
@Route(value = "completed", layout = MainLayout.class)
@PageTitle("Completed | Nei")
public class CompletedView extends NeiView {
    private final TaskUiService tasks;
    private String mode = "Day"; // Day | Week | Month

    public CompletedView(TaskUiService tasks) {
        this.tasks = tasks;
        refresh();
    }

    @Override
    protected void refresh() {
        removeAll();
        add(build());
    }

    private Component build() {
        Div page = new Div();
        page.addClassName("nei-cl");
        page.add(buildBar(), buildList());
        return page;
    }

    private Component buildBar() {
        Div bar = new Div();
        bar.addClassName("nei-cl-bar");

        Span title = new Span("Completed");
        title.addClassName("nei-cl-title");

        Div seg = new Div();
        seg.addClassName("nei-cl-seg");
        for (String m : new String[]{"Day", "Week", "Month"}) {
            Span b = new Span(m);
            b.addClassName("nei-cl-segbtn");
            if (m.equals(mode)) b.addClassName("on");
            b.getElement().addEventListener("click", ev -> {
                mode = m;
                refresh();
            });
            seg.add(b);
        }

        bar.add(title, seg);
        return bar;
    }

    private Component buildList() {
        Div list = new Div();
        list.addClassName("nei-cl-list");

        List<TaskItem> done = new ArrayList<>();
        tasks.all().stream().filter(t -> t.done).forEach(done::add);
        done.sort(Comparator.comparing((TaskItem t) -> t.due,
                Comparator.nullsLast(Comparator.reverseOrder())));

        if (done.isEmpty()) {
            Div empty = new Div("Nothing completed yet. Finished tasks will show up here.");
            empty.addClassName("nei-cl-empty");
            list.add(empty);
            return list;
        }

        Map<String, List<TaskItem>> groups = new LinkedHashMap<>();
        for (TaskItem t : done) groups.computeIfAbsent(groupLabel(t.due), k -> new ArrayList<>()).add(t);

        boolean first = true;
        for (Map.Entry<String, List<TaskItem>> g : groups.entrySet()) {
            Div head = new Div();
            head.addClassName("nei-cl-head");
            if (!first) {
                Span in = new Span();
                in.addClassName("nei-cl-in");
                head.add(in);
            }
            head.add(new Span(g.getKey()));
            list.add(head);
            g.getValue().forEach(t -> list.add(row(t)));
            first = false;
        }
        return list;
    }

    private Component row(TaskItem t) {
        Div row = new Div();
        row.addClassName("nei-cl-item");

        Span text = new Span(t.title);
        text.addClassName("nei-cl-text");

        Span undo = new Span("Reopen");
        undo.addClassName("nei-cl-undo");
        undo.getElement().addEventListener("click", ev -> {
            t.done = false;
            tasks.save(t);
            refresh();
        });

        row.add(text, undo);
        return row;
    }

    private String groupLabel(LocalDate d) {
        if (d == null) return "Earlier";
        LocalDate today = LocalDate.now();

        if (mode.equals("Week")) {
            LocalDate start = d.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            LocalDate thisWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            if (start.equals(thisWeek)) return "This week";
            if (start.equals(thisWeek.minusWeeks(1))) return "Last week";
            return "Week of " + start.format(DateTimeFormatter.ofPattern("MMM d, yyyy"));
        }
        if (mode.equals("Month")) {
            YearMonth m = YearMonth.from(d), now = YearMonth.from(today);
            if (m.equals(now)) return "This month";
            if (m.equals(now.minusMonths(1))) return "Last month";
            return d.format(DateTimeFormatter.ofPattern("MMMM yyyy"));
        }
        if (d.equals(today)) return "Today";
        if (d.equals(today.minusDays(1))) return "Yesterday";
        return d.format(DateTimeFormatter.ofPattern("MMM d, yyyy"));
    }
}