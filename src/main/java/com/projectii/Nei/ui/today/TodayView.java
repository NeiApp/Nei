package com.projectii.Nei.ui.today;

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

@Route(value = "today", layout = MainLayout.class)
@PageTitle("Today | Nei")
public class TodayView extends NeiView {
    private final TaskUiService tasks;
    private final SettingsUiService settings;
    private final UiState state;

    public TodayView(TaskUiService tasks, SettingsUiService settings, UiState state) {
        this.tasks = tasks;
        this.settings = settings;
        this.state = state;
        if (state.selected == null && !tasks.all().isEmpty()) state.selected = tasks.all().get(0);
        refresh();
    }

    @Override
    protected void refresh() {
        removeAll();
        add(buildToday());
    }

    private void noteDialog(TaskItem t) {
        TaskDialogs.openNote(t, tasks, this::refresh);
    }

    private void alarmDialog(TaskItem t) {
        TaskDialogs.openAlarm(t, tasks, settings, this::refresh);
    }

    private Component buildToday() {
        Div wrap = new Div(buildMenu(), buildTaskColumn(), buildDetail());
        wrap.addClassName("nei-today");
        return wrap;
    }

    private Component buildMenu() {
        Div menu = new Div();
        menu.addClassName("nei-menu");
        menu.add(new H4("Menu"));

        TextField search = new TextField();
        search.setPlaceholder("Search");
        search.setPrefixComponent(new Icon(VaadinIcon.SEARCH));
        search.setWidthFull();
        menu.add(search);

        menu.add(sectionLabel("Tasks"),
                menuRow(VaadinIcon.ANGLE_DOUBLE_RIGHT, "Upcoming", 12, null, false),
                menuRow(VaadinIcon.LIST_UL, "Today", openCount(null), null, state.listFilter == null),
                menuRow(VaadinIcon.CALENDAR_O, "Calendar", -1, null, false),
                menuRow(VaadinIcon.NOTEBOOK, "Notes", -1, null, false));

        menu.add(sectionLabel("Lists"));
        menu.add(listRow("Personal", "#ef6f6c"), listRow("Work", "#4cc3d9"));
        menu.add(menuRow(VaadinIcon.PLUS, "Add new list", -1, null, false));

        menu.add(sectionLabel("Tags"));
        Div tags = new Div(tag("Tag 1", "tag-a"), tag("Tag 2", "tag-b"), tag("+ Add tag", "tag-c"));
        tags.addClassName("nei-tags");
        menu.add(tags);

        Div foot = new Div(menuRow(VaadinIcon.COG_O, "Settings", -1, null, false),
                menuRow(VaadinIcon.SIGN_OUT, "Sign out", -1, null, false));
        foot.addClassName("nei-menu-foot");
        menu.add(foot);
        return menu;
    }

    private int openCount(String list) {
        return (int) tasks.all().stream().filter(t -> !t.done && (list == null || t.list.equals(list))).count();
    }

    private Component listRow(String name, String color) {
        return menuRow(null, name, openCount(name), color, name.equals(state.listFilter));
    }

    private Component menuRow(VaadinIcon icon, String label, int count, String dot, boolean active) {
        Div row = new Div();
        row.addClassName("nei-menu-row");
        if (active) row.addClassName("active");
        if (dot != null) {
            Span s = new Span();
            s.addClassName("nei-dot");
            s.getStyle().set("background", dot);
            row.add(s);
        } else if (icon != null) {
            row.add(new Icon(icon));
        }
        Span text = new Span(label);
        text.addClassName("grow");
        row.add(text);
        if (count >= 0) {
            Span c = new Span(String.valueOf(count));
            c.addClassName("nei-count");
            row.add(c);
        }
        if (dot != null) {
            row.addClickListener(e -> { state.listFilter = label.equals(state.listFilter) ? null : label; refresh(); });
        } else if (label.equals("Settings")) {
            row.addClickListener(e -> { go("settings"); });
        } else if (label.equals("Calendar")) {
            row.addClickListener(e -> { go("calendar"); });
        } else if (label.equals("Notes")) {
            row.addClickListener(e -> { go("notes"); });
        } else if (label.equals("Today")) {
            row.addClickListener(e -> { state.listFilter = null; refresh(); });
        }
        return row;
    }

    private Component sectionLabel(String t) { Div d = new Div(t); d.addClassName("nei-section"); return d; }
    private Span tag(String t, String cls) { Span s = new Span(t); s.addClassNames("nei-tag", cls); return s; }

    private Component buildTaskColumn() {
        Div col = new Div();
        col.addClassName("nei-tasks");

        H1 title = new H1("Today");
        Span count = new Span(String.valueOf(openCount(state.listFilter)));
        count.addClassName("nei-big-count");
        Div head = new Div(title, count);
        head.addClassName("nei-tasks-head");
        col.add(head);
        Div hello = new Div("Good day, " + settings.account().userName + ". Keep the momentum going.");
        hello.addClassName("nei-hello");
        col.add(hello);
        if (openCount(null) > settings.account().maxTasksPerDay) {
            Div warn = new Div("\u26A0 " + openCount(null) + " open tasks is over your limit of " + settings.account().maxTasksPerDay
                    + ". Consider moving one to tomorrow.");
            warn.addClassName("nei-warn");
            col.add(warn);
        }

        TextField add = new TextField();
        add.setPlaceholder("Add new task");
        add.setPrefixComponent(new Icon(VaadinIcon.PLUS));
        add.setWidthFull();
        add.addClassName("nei-add");
        add.getElement().addEventListener("keydown", e -> {
            if (add.getValue().isBlank()) return;
            TaskItem t = new TaskItem(add.getValue().trim(), state.listFilter == null ? "Personal" : state.listFilter, LocalDate.now(), false);
            tasks.add(t);
            state.selected = t;
            refresh();
        }).setFilter("event.key === 'Enter'");
        col.add(add);

        Div list = new Div();
        list.addClassName("nei-task-list");
        tasks.all().stream().filter(t -> state.listFilter == null || t.list.equals(state.listFilter)).forEach(t -> list.add(taskRow(t)));
        col.add(list);
        return col;
    }

    private Component taskRow(TaskItem t) {
        Div row = new Div();
        row.addClassName("nei-task");
        if (t == state.selected) row.addClassName("selected");
        if (t.done) row.addClassName("done");

        Checkbox cb = new Checkbox(t.done);
        cb.addValueChangeListener(e -> { t.done = e.getValue(); tasks.save(t); refresh(); });

        Div main = new Div(new Span(t.title));
        main.addClassName("grow");
        if (!t.subs.isEmpty() || t.due != null) {
            Div meta = new Div();
            meta.addClassName("nei-task-meta");
            if (t.due != null) meta.add(new Span("\uD83D\uDCC5 " + t.due));
            if (!t.subs.isEmpty()) meta.add(new Span(t.subs.size() + " Subtasks"));
            if (t.alarm != null) meta.add(new Span("\u23F0 " + t.alarm + "m"));
            Span dot = new Span(t.list);
            dot.addClassNames("nei-tag", t.list.equals("Work") ? "tag-b" : "tag-d");
            meta.add(dot);
            main.add(meta);
        }
        row.add(cb, main, new Icon(VaadinIcon.ANGLE_RIGHT));
        row.addClickListener(e -> { state.selected = t; refresh(); });
        return row;
    }

    private Component buildDetail() {
        Div panel = new Div();
        panel.addClassName("nei-detail");
        if (state.selected == null) {
            Div empty = new Div("Select a task to see its details.");
            empty.addClassName("nei-empty");
            panel.add(empty);
            return panel;
        }

        panel.add(new H3("Task:"));
        TextField title = new TextField();
        title.setValue(state.selected.title);
        title.setWidthFull();
        TextArea desc = new TextArea();
        desc.setPlaceholder("Description");
        desc.setValue(state.selected.description);
        desc.setWidthFull();
        desc.setMinHeight("110px");

        Select<String> list = new Select<>();
        list.setItems("Personal", "Work");
        list.setValue(state.selected.list);
        DatePicker due = new DatePicker();
        due.setValue(state.selected.due);

        TextField time = new TextField();
        time.setPlaceholder("e.g. 09:00 - 10:00");
        time.setValue(state.selected.time);
        Div props = new Div(propRow("List", list), propRow("Time", time), propRow("Due date", due),
                propRow("Tags", new Div(tag("Tag 1", "tag-a"), tag("+ Add tag", "tag-c"))));
        props.addClassName("nei-props");

        panel.add(title, desc, props, new H3("Subtasks:"));
        Div subs = new Div();
        subs.addClassName("nei-subs");
        subs.add(menuRow(VaadinIcon.PLUS, "Add new subtask", -1, null, false));
        for (SubItem s : state.selected.subs) subs.add(new Checkbox(s.title, s.done));
        panel.add(subs);

        Button noteBtn = new Button("Add note", new Icon(VaadinIcon.PENCIL), e -> noteDialog(state.selected));
        Button alarmBtn = new Button(state.selected.alarm == null ? "Set alarm" : "Alarm: every " + state.selected.alarm + "m",
                new Icon(VaadinIcon.BELL), e -> alarmDialog(state.selected));
        noteBtn.addClassName("nei-btn-ghost");
        alarmBtn.addClassName("nei-btn-ghost");
        Div extras = new Div(noteBtn, alarmBtn);
        extras.addClassName("nei-actions");
        panel.add(extras);
        if (!state.selected.note.isBlank()) {
            Div n = new Div(state.selected.note);
            n.addClassName("nei-note");
            panel.add(n);
        }

        Button delete = new Button("Delete task", e -> {
            if (state.running == state.selected) state.running = null;
            tasks.delete(state.selected);
            state.selected = null;
            refresh();
        });
        delete.addClassName("nei-btn-ghost");
        Button save = new Button("Save changes", e -> {
            state.selected.title = title.getValue();
            state.selected.description = desc.getValue();
            if (list.getValue() != null) state.selected.list = list.getValue();
            state.selected.due = due.getValue();
            state.selected.time = time.getValue();
            tasks.save(state.selected);
            Notification.show("Saved");
            refresh();
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        save.addClassName("nei-btn-primary");
        Div actions = new Div(delete, save);
        actions.addClassName("nei-actions");
        panel.add(actions);
        return panel;
    }

    private Component propRow(String label, Component c) {
        Div r = new Div(new Span(label), c);
        r.addClassName("nei-prop");
        return r;
    }

}
