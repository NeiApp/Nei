package com.projectii.Nei.ui.calendar;

import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.timepicker.TimePicker;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.data.value.ValueChangeMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.function.Consumer;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;
import com.projectii.Nei.ui.model.*;
import com.projectii.Nei.ui.service.*;
import com.projectii.Nei.ui.shared.*;
import com.projectii.Nei.ui.app.MainLayout;

@Route(value = "calendar", layout = MainLayout.class)
@PageTitle("Calendar | Nei")
public class CalendarView extends NeiView {
    private final TaskUiService tasks;
    private final NoteUiService notes;
    private final SettingsUiService settings;
    private final UiState state;

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

    private YearMonth calMonth = YearMonth.now();
    private final java.util.Set<String> calHidden = new java.util.HashSet<>(); // "personal" | "work" | "note"

    private Component buildCalendar() {
        Span title = new Span(calMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")));
        title.addClassName("nei-cal-title");
        Button prev = new Button(new Icon(VaadinIcon.ANGLE_LEFT), e -> { calMonth = calMonth.minusMonths(1); refresh(); });
        Button next = new Button(new Icon(VaadinIcon.ANGLE_RIGHT), e -> { calMonth = calMonth.plusMonths(1); refresh(); });
        prev.getElement().setAttribute("aria-label", "Previous month");
        next.getElement().setAttribute("aria-label", "Next month");
        prev.addClassName("nei-cal-nav");
        next.addClassName("nei-cal-nav");
        Button today = new Button("Today", e -> { calMonth = YearMonth.now(); refresh(); });
        today.addClassName("nei-btn-ghost");
        Div head = new Div(title, prev, next, today);
        head.addClassName("nei-cal-head");

        Button create = new Button("+ Create task", e -> openCalendarAdd(LocalDate.now()));
        create.addClassName("nei-btn-primary");
        Div legend = new Div(new H4("My calendars"),
                legendBox("Personal", "personal"), legendBox("Work", "work"), legendBox("Notes", "note"));
        legend.addClassName("nei-legend-box");
        Div side = new Div(create, legend);
        side.addClassName("nei-cal-side");

        Div grid = new Div();
        grid.addClassName("nei-cal-grid");
        boolean sunFirst = settings.account().weekStart.equals("Sunday");
        for (String d : sunFirst ? new String[]{"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"}
                : new String[]{"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"}) {
            Div h = new Div(d);
            h.addClassName("nei-cal-dow");
            grid.add(h);
        }
        LocalDate first = calMonth.atDay(1);
        int offset = sunFirst ? first.getDayOfWeek().getValue() % 7 : first.getDayOfWeek().getValue() - 1;
        int cells = ((offset + calMonth.lengthOfMonth() + 6) / 7) * 7;
        LocalDate start = first.minusDays(offset);
        for (int i = 0; i < cells; i++) grid.add(dayCell(start.plusDays(i)));

        Div main = new Div(head, grid);
        main.addClassName("nei-cal-main");
        Div page = new Div(side, main);
        page.addClassName("nei-calendar");
        return page;
    }

    private Component legendBox(String label, String kind) {
        Checkbox cb = new Checkbox(label, !calHidden.contains(kind));
        cb.addClassNames("nei-legend", kind);
        cb.addValueChangeListener(e -> {
            if (e.getValue()) calHidden.remove(kind); else calHidden.add(kind);
            refresh();
        });
        return cb;
    }

    private Component dayCell(LocalDate day) {
        Div cell = new Div();
        cell.addClassName("nei-cal-cell");
        if (!YearMonth.from(day).equals(calMonth)) cell.addClassName("out");

        Span num = new Span(String.valueOf(day.getDayOfMonth()));
        num.addClassName("nei-cal-num");
        if (day.equals(LocalDate.now())) num.addClassName("today");
        Button add = new Button(new Icon(VaadinIcon.PLUS), e -> openCalendarAdd(day));
        add.addClassName("nei-cal-add");
        add.getElement().setAttribute("aria-label", "Add task on " + day);
        Div top = new Div(num, add);
        top.addClassName("nei-cal-cell-top");
        cell.add(top);

        List<Component> chips = new ArrayList<>();
        tasks.all().stream().filter(t -> day.equals(t.due))
                .filter(t -> !calHidden.contains(t.list.equals("Work") ? "work" : "personal"))
                .forEach(t -> chips.add(taskChip(t)));
        if (!calHidden.contains("note")) {
            notes.all().stream().filter(n -> n.date.equals(day)).forEach(n -> chips.add(noteChip(n)));
        }
        chips.stream().limit(3).forEach(c -> cell.add(c));
        if (chips.size() > 3) {
            Div more = new Div("+" + (chips.size() - 3) + " more");
            more.addClassName("nei-cal-more");
            cell.add(more);
        }
        return cell;
    }

    private Component taskChip(TaskItem t) {
        String start = t.time.isBlank() ? "" : t.time.split(" ")[0] + " ";
        Div chip = new Div(start + t.title);
        chip.addClassNames("nei-cal-chip", t.list.equals("Work") ? "work" : "personal");
        if (t.done) chip.addClassName("done");
        chip.getElement().setAttribute("title", t.title);
        chip.addClickListener(e -> { state.selected = t; state.listFilter = null; go("today"); });
        return chip;
    }

    private Component noteChip(NoteItem n) {
        Div chip = new Div("Note: " + n.text);
        chip.addClassNames("nei-cal-chip", "note");
        chip.addClickListener(e -> { go("notes"); });
        return chip;
    }

    private void openCalendarAdd(LocalDate day) {
        Dialog d = new Dialog();
        d.setHeaderTitle("New task on " + day.format(DateTimeFormatter.ofPattern("EEE, MMM d")));
        TextField title = new TextField("Title");
        title.setWidthFull();
        TextField time = new TextField("Time (optional)");
        time.setPlaceholder("e.g. 09:00 - 10:00");
        time.setWidthFull();
        Select<String> list = new Select<>();
        list.setLabel("List");
        list.setItems("Personal", "Work");
        list.setValue("Personal");
        Button save = new Button("Save", e -> {
            if (title.getValue().isBlank()) { Notification.show("Title is required"); return; }
            TaskItem t = new TaskItem(title.getValue().trim(), list.getValue(), day, false); // MOCK: taskService.createTask(...)
            t.time = time.getValue();
            tasks.add(t);
            d.close();
            refresh();
        });
        save.addClassName("nei-btn-primary");
        Div form = new Div(title, time, list);
        form.addClassName("nei-form");
        d.add(form);
        d.getFooter().add(new Button("Cancel", e -> d.close()), save);
        d.open();
    }
}
