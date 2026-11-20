package com.projectii.Nei.ui.calendar;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.projectii.Nei.ui.app.MainLayout;
import com.projectii.Nei.ui.model.*;
import com.projectii.Nei.ui.service.*;
import com.projectii.Nei.ui.shared.*;

@CssImport("./styles/calendar.css")
@Route(value = "calendar", layout = MainLayout.class)
@PageTitle("Calendar | Nei")
public class CalendarView extends NeiView {
    private final TaskUiService tasks;
    private final NoteUiService notes;
    private final SettingsUiService settings;
    private final UiState state;

    private YearMonth calMonth = YearMonth.now();
    private LocalDate selectedDay = LocalDate.now();
    private final Set<String> calHidden = new HashSet<>(); // "personal" | "work" | "note"

    public CalendarView(TaskUiService tasks, NoteUiService notes, SettingsUiService settings, UiState state) {
        this.tasks = tasks;
        this.notes = notes;
        this.settings = settings;
        this.state = state;
        refresh();
    }

    @Override
    protected void refresh() {
        removeAll();
        add(buildCalendar());
    }

    private Component buildCalendar() {
        Div page = new Div();
        page.addClassName("nei-calendar");
        page.add(buildCalMain());
        page.add(buildDayPanel());
        return page;
    }

    private Component buildCalMain() {
        Div main = new Div();
        main.addClassName("nei-cal-main");
        main.add(buildToolbar());
        main.add(buildGrid());
        return main;
    }

    private Component buildToolbar() {
        Div toolbar = new Div();
        toolbar.addClassName("nei-cal-toolbar");

        Div left = new Div();
        left.addClassName("nei-cal-toolbar-left");

        Span title = new Span(calMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")));
        title.addClassName("nei-cal-title");

        Button prev = new Button();
        prev.setIcon(new Icon(VaadinIcon.ANGLE_LEFT));
        prev.addClassName("nei-cal-nav");
        prev.getElement().setAttribute("aria-label", "Previous month");
        prev.addClickListener(e -> {
            calMonth = calMonth.minusMonths(1);
            refresh();
        });

        Button next = new Button();
        next.setIcon(new Icon(VaadinIcon.ANGLE_RIGHT));
        next.addClassName("nei-cal-nav");
        next.getElement().setAttribute("aria-label", "Next month");
        next.addClickListener(e -> {
            calMonth = calMonth.plusMonths(1);
            refresh();
        });

        Button todayBtn = new Button();
        todayBtn.setText("Today");
        todayBtn.addClassName("nei-btn-ghost");
        todayBtn.addClickListener(e -> {
            calMonth = YearMonth.now();
            selectedDay = LocalDate.now();
            refresh();
        });

        left.add(title);
        left.add(prev);
        left.add(next);
        left.add(todayBtn);

        Div filters = new Div();
        filters.addClassName("nei-cal-filters");
        filters.add(filterPill("Personal", "personal"));
        filters.add(filterPill("Work", "work"));
        filters.add(filterPill("Notes", "note"));

        toolbar.add(left);
        toolbar.add(filters);
        return toolbar;
    }

    private Component filterPill(String label, String kind) {
        Div pill = new Div();
        pill.addClassName("nei-cal-filter");
        pill.addClassName(kind);

        if (!calHidden.contains(kind)) {
            pill.addClassName("on");
        }

        Span text = new Span(label);
        pill.add(text);

        pill.getElement().setAttribute("role", "button");
        pill.getElement().setAttribute("aria-pressed", String.valueOf(!calHidden.contains(kind)));

        pill.getElement().addEventListener("click", ev -> {
            if (calHidden.contains(kind)) {
                calHidden.remove(kind);
            } else {
                calHidden.add(kind);
            }
            refresh();
        });

        return pill;
    }

    private Component buildGrid() {
        Div grid = new Div();
        grid.addClassName("nei-cal-grid");

        boolean sunFirst = "Sunday".equals(settings.account().weekStart);
        String[] days = sunFirst
                ? new String[]{"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"}
                : new String[]{"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};

        for (String d : days) {
            Div h = new Div();
            h.addClassName("nei-cal-dow");
            h.add(new Span(d));
            grid.add(h);
        }

        LocalDate first = calMonth.atDay(1);
        int dow = first.getDayOfWeek().getValue(); // Mon=1 ... Sun=7
        int offset = sunFirst ? dow % 7 : dow - 1;
        int cells = ((offset + calMonth.lengthOfMonth() + 6) / 7) * 7;
        LocalDate start = first.minusDays(offset);

        for (int i = 0; i < cells; i++) {
            grid.add(dayCell(start.plusDays(i)));
        }

        return grid;
    }

    private Component dayCell(LocalDate day) {
        Div cell = new Div();
        cell.addClassName("nei-cal-cell");

        if (!YearMonth.from(day).equals(calMonth)) {
            cell.addClassName("out");
        }

        if (day.equals(selectedDay)) {
            cell.addClassName("selected");
        }

        Div top = new Div();
        top.addClassName("nei-cal-cell-top");

        Span num = new Span(String.valueOf(day.getDayOfMonth()));
        num.addClassName("nei-cal-num");
        if (day.equals(LocalDate.now())) {
            num.addClassName("today");
        }

        Button add = new Button();
        add.setIcon(new Icon(VaadinIcon.PLUS));
        add.addClassName("nei-cal-add");
        add.getElement().setAttribute("aria-label", "Add task on " + day);
        add.addClickListener(e -> openCalendarAdd(day));

        top.add(num);
        top.add(add);
        cell.add(top);

        List<Component> chips = new ArrayList<>();

        tasks.all().stream()
                .filter(t -> day.equals(t.due))
                .filter(t -> !calHidden.contains(categoryForTask(t)))
                .forEach(t -> chips.add(taskChip(t)));

        if (!calHidden.contains("note")) {
            notes.all().stream()
                    .filter(n -> day.equals(n.date))
                    .forEach(n -> chips.add(noteChip(n)));
        }

        int limit = Math.min(3, chips.size());
        for (int i = 0; i < limit; i++) {
            cell.add(chips.get(i));
        }

        if (chips.size() > 3) {
            Div more = new Div();
            more.addClassName("nei-cal-more");
            more.add(new Span("+" + (chips.size() - 3) + " more"));
            cell.add(more);
        }

        cell.getElement().addEventListener("click", ev -> {
            selectedDay = day;
            refresh();
        });

        return cell;
    }

    private Component taskChip(TaskItem t) {
        Div chip = new Div();
        chip.addClassName("nei-cal-chip");
        chip.addClassName(categoryForTask(t));

        if (t.done) {
            chip.addClassName("done");
        }

        String time = t.time == null ? "" : t.time;
        String start = time.isBlank() ? "" : time.split(" ")[0] + " ";
        chip.add(new Span(start + t.title));
        chip.getElement().setAttribute("title", t.title);

        chip.getElement().addEventListener("click", ev -> {
            selectedDay = t.due;
            state.selected = t;
            refresh();
        });

        return chip;
    }

    private Component noteChip(NoteItem n) {
        Div chip = new Div();
        chip.addClassName("nei-cal-chip");
        chip.addClassName("note");

        String text = n.text == null ? "" : n.text;
        chip.add(new Span("Note: " + truncate(text, 28)));
        chip.getElement().setAttribute("title", text);

        chip.getElement().addEventListener("click", ev -> {
            selectedDay = n.date;
            refresh();
        });

        return chip;
    }

    private Component buildDayPanel() {
        Div panel = new Div();
        panel.addClassName("nei-cal-daypanel");

        Div head = new Div();
        head.addClassName("nei-cal-day-head");

        Span date = new Span(selectedDay.format(DateTimeFormatter.ofPattern("EEEE, MMM d")));
        date.addClassName("nei-cal-day-date");

        Button addBtn = new Button();
        addBtn.setText("+ Add task");
        addBtn.addClassName("nei-btn-primary");
        addBtn.addClickListener(e -> openCalendarAdd(selectedDay));

        head.add(date);
        head.add(addBtn);
        panel.add(head);

        Div body = new Div();
        body.addClassName("nei-cal-day-body");

        List<TaskItem> dayTasks = new ArrayList<>();
        tasks.all().stream()
                .filter(t -> selectedDay.equals(t.due))
                .filter(t -> !calHidden.contains(categoryForTask(t)))
                .forEach(dayTasks::add);

        List<NoteItem> dayNotes = new ArrayList<>();
        if (!calHidden.contains("note")) {
            notes.all().stream()
                    .filter(n -> selectedDay.equals(n.date))
                    .forEach(dayNotes::add);
        }

        if (!dayTasks.isEmpty()) {
            Span label = new Span("Tasks");
            label.addClassName("nei-cal-day-label");
            body.add(label);

            for (TaskItem t : dayTasks) {
                body.add(dayTaskRow(t));
            }
        }

        if (!dayNotes.isEmpty()) {
            Span label = new Span("Notes");
            label.addClassName("nei-cal-day-label");
            body.add(label);

            for (NoteItem n : dayNotes) {
                body.add(dayNoteRow(n));
            }
        }

        if (dayTasks.isEmpty() && dayNotes.isEmpty()) {
            Div empty = new Div();
            empty.addClassName("nei-cal-day-empty");

            Span title = new Span("Nothing scheduled.");
            Span hint = new Span("A calm day is allowed.");

            empty.add(title);
            empty.add(hint);
            body.add(empty);
        }

        panel.add(body);
        return panel;
    }

    private Component dayTaskRow(TaskItem t) {
        Div row = new Div();
        row.addClassName("nei-cal-day-row");

        if (t == state.selected) {
            row.addClassName("selected");
        }

        if (t.done) {
            row.addClassName("done");
        }

        Checkbox cb = new Checkbox();
        cb.setValue(t.done);
        cb.addValueChangeListener(e -> {
            t.done = e.getValue();
            tasks.save(t);
            refresh();
        });

        Div text = new Div();
        text.addClassName("nei-cal-day-text");

        Span title = new Span(t.title);
        title.addClassName("nei-cal-day-title");
        text.add(title);

        String time = t.time == null ? "" : t.time;
        if (!time.isBlank()) {
            Span tm = new Span(time);
            tm.addClassName("nei-cal-day-time");
            text.add(tm);
        }

        row.add(cb);
        row.add(text);

        row.getElement().addEventListener("click", ev -> {
            state.selected = t;
            refresh();
        });

        return row;
    }

    private Component dayNoteRow(NoteItem n) {
        Div row = new Div();
        row.addClassName("nei-cal-day-note");
        row.add(new Span(n.text == null ? "" : n.text));

        row.getElement().addEventListener("click", ev -> go("notes"));

        return row;
    }

    private void openCalendarAdd(LocalDate day) {
        Dialog d = new Dialog();
        d.setHeaderTitle("New task on " + day.format(DateTimeFormatter.ofPattern("EEE, MMM d")));

        TextField title = new TextField();
        title.setLabel("Title");
        title.setWidthFull();

        TextField time = new TextField();
        time.setLabel("Time (optional)");
        time.setPlaceholder("e.g. 09:00 - 10:00");
        time.setWidthFull();

        Select<String> list = new Select<>();
        list.setLabel("List");
        list.setItems("Personal", "Work");
        list.setValue("Personal");

        Button save = new Button();
        save.setText("Save");
        save.addClassName("nei-btn-primary");
        save.addClickListener(e -> {
            if (title.getValue().isBlank()) {
                com.vaadin.flow.component.notification.Notification.show("Title is required");
                return;
            }

            String chosenList = list.getValue() == null ? "Personal" : list.getValue();
            TaskItem t = new TaskItem(title.getValue().trim(), chosenList, day, false);
            t.time = time.getValue();
            tasks.add(t);

            selectedDay = day;
            calMonth = YearMonth.from(day);
            d.close();
            refresh();
        });

        Button cancel = new Button();
        cancel.setText("Cancel");
        cancel.addClickListener(e -> d.close());

        Div form = new Div();
        form.addClassName("nei-form");
        form.add(title);
        form.add(time);
        form.add(list);

        d.add(form);
        d.getFooter().add(cancel);
        d.getFooter().add(save);
        d.open();
    }

    private String categoryForTask(TaskItem t) {
        return t != null && "Work".equals(t.list) ? "work" : "personal";
    }

    private String truncate(String s, int max) {
        if (s == null) {
            return "";
        }

        String v = s.strip();
        return v.length() <= max ? v : v.substring(0, max) + "…";
    }
}