package com.projectii.Nei.ui.today;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.data.value.ValueChangeMode;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.projectii.Nei.ui.model.*;
import com.projectii.Nei.ui.service.*;
import com.projectii.Nei.ui.shared.*;
import com.projectii.Nei.ui.app.MainLayout;

@CssImport("./styles/today.css")
@Route(value = "today", layout = MainLayout.class)
@PageTitle("Today | Nei")
public class TodayView extends NeiView {
    private final TaskUiService tasks;
    private final SettingsUiService settings;
    private final UiState state;
    private String query = "";

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

    private void alarmDialog(TaskItem t) {
        TaskDialogs.openAlarm(t, tasks, settings, this::refresh);
    }

    private Component buildToday() {
        Div wrap = new Div(buildMain(), buildDetail());
        wrap.addClassName("nei-today");
        return wrap;
    }

    // ---------------- LEFT ZONE: header bar + add + list ----------------

    private Component buildMain() {
        Div main = new Div();
        main.addClassName("nei-today-main");
        main.add(buildBar(), buildAddRow(), buildList());
        return main;
    }

    private Component buildBar() {
        Div bar = new Div();
        bar.addClassName("nei-today-bar");

        Div left = new Div();
        left.addClassName("nei-bar-left");
        Span greet = new Span("Good day, " + settings.account().userName + ".");
        greet.addClassName("nei-greet");
        Span sub = new Span("Keep the momentum going.");
        sub.addClassName("nei-greet-sub");
        Span date = new Span(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMM d")));
        date.addClassName("nei-date");
        left.add(greet, sub, date);

        Div right = new Div();
        right.addClassName("nei-bar-right");

        TextField search = new TextField();
        search.addClassName("nei-bar-search");
        search.setPlaceholder("Search tasks");
        search.setPrefixComponent(new Icon(VaadinIcon.SEARCH));
        search.setValue(query);
        search.setValueChangeMode(ValueChangeMode.EAGER);
        search.addValueChangeListener(e -> {
            query = e.getValue() == null ? "" : e.getValue();
            refresh();
        });

        Div pills = new Div();
        pills.addClassName("nei-pills");
        pills.add(pill("All", null));
        pills.add(pill("Personal", "Personal"));
        pills.add(pill("Work", "Work"));

        Span count = new Span(String.valueOf(openCount(state.listFilter)));
        count.addClassName("nei-count2");
        count.getElement().setAttribute("title", "open tasks");

        right.add(search, pills, count);
        if (openCount(null) > settings.account().maxTasksPerDay) {
            Span over = new Span("⚠ over limit");
            over.addClassName("nei-over");
            right.add(over);
        }
        bar.add(left, right);
        return bar;
    }

    private Component pill(String label, String list) {
        Span p = new Span(label);
        p.addClassName("nei-pillf");
        boolean active = (list == null && state.listFilter == null) || (list != null && list.equals(state.listFilter));
        if (active) p.addClassName("on");
        p.getElement().addEventListener("click", ev -> {
            state.listFilter = list;
            refresh();
        });
        return p;
    }

    private Component buildAddRow() {
        Div row = new Div();
        row.addClassName("nei-add-row");

        TextField add = new TextField();
        add.addClassName("nei-add");
        add.setPlaceholder("Add new task");
        add.setPrefixComponent(new Icon(VaadinIcon.PLUS));
        add.setWidthFull();

        Button addBtn = new Button();
        addBtn.setIcon(new Icon(VaadinIcon.PLUS));
        addBtn.addClassName("nei-add-btn");
        addBtn.getElement().setAttribute("aria-label", "Add task");
        addBtn.addClickListener(e -> addTask(add));

        add.getElement()
                .addEventListener("keydown", ev -> addTask(add))
                .setFilter("event.key === 'Enter'");

        row.add(add);
        row.add(addBtn);
        return row;
    }

    private void addTask(TextField field) {
        String value = field.getValue();
        if (value == null || value.isBlank()) {
            Notification.show("Type a task first");
            return;
        }

        String list = state.listFilter == null ? "Personal" : state.listFilter;
        TaskItem t = new TaskItem(value.trim(), list, LocalDate.now(), false);
        tasks.add(t);
        state.selected = t;
        refresh();
    }

    private Component buildList() {
        Div list = new Div();
        list.addClassName("nei-task-list");

        String q = query.toLowerCase();
        List<TaskItem> rows = new ArrayList<>();
        tasks.all().stream()
                .filter(t -> state.listFilter == null || t.list.equals(state.listFilter))
                .filter(t -> q.isBlank() || contains(t.title, q) || contains(t.note, q) || contains(t.description, q))
                .forEach(rows::add);

        rows.sort((a, b) -> Boolean.compare(a.done, b.done)); // open first, done sink to the bottom

        if (rows.isEmpty()) {
            Div empty = new Div(q.isBlank() ? "Nothing here — add a task above." : "No tasks match “" + query + "”.");
            empty.addClassName("nei-empty");
            list.add(empty);
        } else {
            rows.forEach(t -> list.add(taskRow(t)));
        }
        return list;
    }

    private boolean contains(String s, String q) {
        return s != null && s.toLowerCase().contains(q);
    }

    private Component taskRow(TaskItem t) {
        Div row = new Div();
        row.addClassName("nei-task");
        if (t == state.selected) row.addClassName("selected");
        if (t.done) row.addClassName("done");

        Checkbox cb = new Checkbox();
        cb.setValue(t.done);
        cb.addValueChangeListener(e -> {
            t.done = e.getValue();
            tasks.save(t);
            refresh();
        });

        Div body = new Div();
        body.addClassName("nei-task-body");
        Span title = new Span(t.title);
        title.addClassName("nei-task-title");
        body.add(title);

        Div flags = new Div();
        flags.addClassName("nei-task-flags");

        if (t.due != null) {
            Span f = new Span(t.due.format(DateTimeFormatter.ofPattern("MMM d")));
            f.addClassName("nei-flag");
            f.addClassName("nei-flag-date");
            flags.add(f);
        }

        Span lf = new Span(t.list);
        lf.addClassName("nei-flag");
        lf.addClassName("nei-flag-list");
        lf.addClassName("Work".equals(t.list) ? "is-work" : "is-personal");
        flags.add(lf);

        if (!t.subs.isEmpty()) {
            Span f = new Span(t.subs.size() + " sub");
            f.addClassName("nei-flag");
            flags.add(f);
        }

        if (t.alarm != null) {
            Span f = new Span("⏰ " + t.alarm + "m");
            f.addClassName("nei-flag");
            f.addClassName("nei-flag-alarm");
            flags.add(f);
        }

        if (flags.getChildren().findAny().isPresent()) body.add(flags);

        Icon chevron = new Icon(VaadinIcon.ANGLE_RIGHT);
        chevron.addClassName("nei-task-go");

        row.add(cb, body, chevron);
        row.addClickListener(e -> {
            state.selected = t;
            refresh();
        });
        return row;
    }

    // ---------------- RIGHT ZONE: inspector (never dead) ----------------

    private Component buildDetail() {
        Div panel = new Div();
        panel.addClassName("nei-detail");
        if (state.selected == null) panel.add(detEmpty());
        else panel.add(detEditor(state.selected));
        return panel;
    }

    private Component detEmpty() {
        Div wrap = new Div();
        wrap.addClassName("nei-det-empty");

        Div cat = new Div();
        cat.addClassName("nei-det-empty-cat");

        Span head = new Span("Today at a glance");
        head.addClassName("nei-det-empty-head");

        Div glance = new Div();
        glance.addClassName("nei-det-glance");
        glance.add(statLine("#ef6f6c", "Personal", openCount("Personal")));
        glance.add(statLine("#4cc3d9", "Work", openCount("Work")));
        glance.add(statLine("var(--nei-muted)", "All open", openCount(null)));

        Span hint = new Span("Select a task to edit it here.");
        hint.addClassName("nei-det-hint");

        wrap.add(cat, head, glance, hint);

        if (openCount(null) > settings.account().maxTasksPerDay) {
            Div warn = new Div("You're over your daily limit of " + settings.account().maxTasksPerDay
                    + ". Consider moving one to tomorrow.");
            warn.addClassName("nei-warn");
            wrap.add(warn);
        }
        return wrap;
    }

    private Component statLine(String color, String label, int n) {
        Dot dot = new Dot(color);
        Span l = new Span(label);
        Span c = new Span(String.valueOf(n));
        c.addClassName("nei-stat-n");
        Div row = new Div(dot, l, c);
        row.addClassName("nei-stat-line");
        return row;
    }

    /** tiny coloured dot span (inline bg, so css stays colour-agnostic) */
    private static final class Dot extends Span {
        Dot(String color) {
            addClassName("nei-dot");
            getStyle().set("background", color);
        }
    }

    private Component detEditor(TaskItem t) {
        Div wrap = new Div();
        wrap.addClassName("nei-det-editor");

        TextField title = new TextField();
        title.setValue(t.title);
        title.setWidthFull();
        title.addClassName("nei-det-title");

        TextArea desc = new TextArea();
        desc.setPlaceholder("Description");
        desc.setValue(t.description);
        desc.setWidthFull();
        desc.setMinHeight("90px");
        desc.addClassName("nei-det-desc");

        Select<String> list = new Select<>();
        list.setItems("Personal", "Work");
        list.setValue(t.list);

        DatePicker due = new DatePicker();
        due.setValue(t.due);

        TextField time = new TextField();
        time.setPlaceholder("e.g. 09:00 - 10:00");
        time.setValue(t.time);

        Div tags = new Div(tag("Tag 1", "tag-a"), tag("+ Add tag", "tag-c"));
        tags.addClassName("nei-tags");

        Div addSub = new Div(new Icon(VaadinIcon.PLUS), new Span("Add subtask"));
        addSub.addClassName("nei-det-addsub");

        Div subs = new Div();
        subs.addClassName("nei-subs");
        for (SubItem s : t.subs) {
            Div subRow = new Div();
            subRow.addClassName("nei-sub-row");
            Checkbox cb = new Checkbox();
            cb.setValue(s.done);
            subRow.add(cb, new Span(s.title));
            subs.add(subRow);
        }

        String note = t.note == null ? "" : t.note;

        Button noteBtn = new Button();
        noteBtn.setText(note.isBlank() ? "Open Notes" : "Open in Notes");
        noteBtn.setIcon(new Icon(VaadinIcon.NOTEBOOK));
        noteBtn.addClassName("nei-btn-ghost");
        noteBtn.addClickListener(e -> {
            state.selected = t;
            go("notes");
        });

        Div noteSec = new Div(noteBtn);
        if (!note.isBlank()) {
            Div n = new Div(note);
            n.addClassName("nei-note");
            noteSec.add(n);
        }

        Button alarmBtn = new Button();
        alarmBtn.setText(t.alarm == null ? "Set reminder" : "Reminder: every " + t.alarm + "m");
        alarmBtn.setIcon(new Icon(VaadinIcon.BELL));
        alarmBtn.addClassName("nei-btn-ghost");
        alarmBtn.addClickListener(e -> alarmDialog(t));

        Button del = new Button();
        del.setText("Delete");
        del.addClassName("nei-btn-ghost");
        del.addClickListener(e -> {
            if (state.running == t) state.running = null;
            tasks.delete(t);
            state.selected = null;
            refresh();
        });

        Button save = new Button();
        save.setText("Save changes");
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        save.addClassName("nei-btn-primary");
        save.addClickListener(e -> {
            t.title = title.getValue();
            t.description = desc.getValue();
            if (list.getValue() != null) t.list = list.getValue();
            t.due = due.getValue();
            t.time = time.getValue();
            tasks.save(t);
            Notification.show("Saved");
            refresh();
        });

        Div actions = new Div(del, save);
        actions.addClassName("nei-actions");

        wrap.add(
                sec("Title", title),
                sec("Description", desc),
                sec("When", propRow("Time", time), propRow("Due date", due)),
                sec("Where", propRow("List", list), tagsBlock("Tags", tags)),
                sec("Subtasks", addSub, subs),
                sec("Note", noteSec),
                sec("Reminder", alarmBtn),
                actions);
        return wrap;
    }

    private Component sec(String label, Component... body) {
        Div s = new Div();
        s.addClassName("nei-det-sec");
        Span l = new Span(label);
        l.addClassName("nei-det-label");
        s.add(l);
        for (Component c : body) s.add(c);
        return s;
    }

    private Component tagsBlock(String label, Div tags) {
        Div w = new Div();
        w.addClassName("nei-prop");
        Span l = new Span(label);
        w.add(l, tags);
        return w;
    }

    private Component propRow(String label, Component c) {
        Div r = new Div(new Span(label), c);
        r.addClassName("nei-prop");
        return r;
    }

    private int openCount(String list) {
        return (int) tasks.all().stream()
                .filter(x -> !x.done && (list == null || x.list.equals(list)))
                .count();
    }

    private Span tag(String t, String cls) {
        Span s = new Span(t);
        s.addClassName("nei-tag");
        s.addClassName(cls);
        return s;
    }
}