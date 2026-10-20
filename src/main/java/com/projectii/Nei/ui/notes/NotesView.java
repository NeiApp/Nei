package com.projectii.Nei.ui.notes;

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

@Route(value = "notes", layout = MainLayout.class)
@PageTitle("Notes | Nei")
public class NotesView extends NeiView {
    private final TaskUiService tasks;
    private final NoteUiService notes;
    private String noteColorFilter = null;
    private String noteSearch = "";

    public NotesView(TaskUiService tasks, NoteUiService notes) {
        this.tasks = tasks;
        this.notes = notes;
        refresh();
    }

    @Override
    protected void refresh() {
        removeAll();
        add(buildNotes());
    }

    private void noteDialog(TaskItem t) {
        TaskDialogs.openNote(t, tasks, this::refresh);
    }

    private record NoteView(String text, String color, LocalDate date, NoteItem note, TaskItem task) {}

    private Component buildNotes() {
        Div grid = new Div();
        grid.addClassName("nei-note-grid");
        Runnable fill = () -> {
            grid.removeAll();
            String q = noteSearch.toLowerCase();
            List<NoteView> all = new ArrayList<>();
            notes.all().forEach(n -> all.add(new NoteView(n.text, n.color, n.date, n, null)));
            // notes written on tasks show up here automatically
            tasks.all().stream().filter(t -> !t.note.isBlank()).forEach(t -> all.add(new NoteView(t.note,
                    t.list.equals("Work") ? Palette.NOTE_COLORS[3] : Palette.NOTE_COLORS[0],
                    t.due != null ? t.due : LocalDate.now(), null, t)));
            all.stream()
                    .filter(v -> noteColorFilter == null || v.color().equals(noteColorFilter))
                    .filter(v -> q.isBlank() || v.text().toLowerCase().contains(q)
                            || (v.task() != null && v.task().title.toLowerCase().contains(q)))
                    .sorted(java.util.Comparator.comparing(NoteView::date).reversed())
                    .forEach(v -> grid.add(noteCard(v)));
            if (grid.getChildren().findAny().isEmpty()) {
                Div empty = new Div("No notes yet. Tap + to write about your day.");
                empty.addClassName("nei-empty");
                grid.add(empty);
            }
        };

        TextField search = new TextField();
        search.addClassName("nei-note-search");
        search.setPlaceholder("Search notes");
        search.setPrefixComponent(new Icon(VaadinIcon.SEARCH));
        search.setValue(noteSearch);
        search.setValueChangeMode(ValueChangeMode.EAGER);
        search.addValueChangeListener(e -> { noteSearch = e.getValue(); fill.run(); });

        Div dots = new Div();
        dots.addClassName("nei-note-dots");
        for (String c : Palette.NOTE_COLORS) {
            Span dot = new Span();
            dot.addClassName("nei-cdot");
            if (c.equals(noteColorFilter)) dot.addClassName("on");
            dot.getStyle().set("background", c);
            dot.addClickListener(e -> { noteColorFilter = c.equals(noteColorFilter) ? null : c; refresh(); });
            dots.add(dot);
        }

        Button plus = new Button(new Icon(VaadinIcon.PLUS), e -> openNoteEditor(null));
        plus.addClassNames("nei-round", "nei-note-plus");
        plus.getElement().setAttribute("title", "New note");
        plus.getElement().setAttribute("aria-label", "New note");

        Div tools = new Div(search, dots, plus);
        tools.addClassName("nei-note-tools");
        Div head = new Div(new H1("Notes"), tools);
        head.addClassName("nei-note-head");

        fill.run();
        Div page = new Div(head, grid);
        page.addClassName("nei-notes");
        return page;
    }

    private Component noteCard(NoteView v) {
        Div card = new Div();
        card.addClassName("nei-note-card");
        card.getStyle().set("background", v.color());

        if (v.task() != null) {
            Span chip = new Span("Task: " + v.task().title);
            chip.addClassName("nei-note-chip");
            card.add(chip);
        } else {
            NoteItem n = v.note();
            Button star = new Button(new Icon(VaadinIcon.STAR), e -> { n.starred = !n.starred; notes.save(n); refresh(); });
            star.addClassNames("nei-round", "nei-star", n.starred ? "on" : "off");
            star.getElement().setAttribute("aria-label", "Favourite");
            card.add(star);
        }

        Div text = new Div(v.text());
        text.addClassName("nei-note-text");

        Button edit = new Button(new Icon(VaadinIcon.PENCIL), e -> {
            if (v.note() != null) openNoteEditor(v.note());
            else noteDialog(v.task());
        });
        edit.addClassName("nei-round");
        edit.getElement().setAttribute("aria-label", "Open note");
        Div foot = new Div(new Span(v.date().format(DateTimeFormatter.ofPattern("MMM d, yyyy"))), edit);
        foot.addClassName("nei-note-foot");

        card.add(text, foot);
        return card;
    }

    private void openNoteEditor(NoteItem existing) {
        Dialog d = new Dialog();
        d.setHeaderTitle(existing == null ? "New note" : "Edit note");
        TextArea text = new TextArea();
        text.setPlaceholder("How was your day?");
        text.setWidth("380px");
        text.setMinHeight("160px");
        text.setMaxHeight("300px");
        text.setMaxLength(2000);
        if (existing != null) text.setValue(existing.text);

        String[] picked = {existing != null ? existing.color : Palette.NOTE_COLORS[0]};
        Div swatches = new Div();
        swatches.addClassName("nei-swatches");
        for (String c : Palette.NOTE_COLORS) {
            Span sw = new Span();
            sw.addClassName("nei-cdot");
            sw.getStyle().set("background", c);
            if (c.equals(picked[0])) sw.addClassName("on");
            sw.addClickListener(e -> {
                picked[0] = c; // MOCK: replace with noteService call
                swatches.getChildren().forEach(x -> x.getElement().getClassList().remove("on"));
                sw.addClassName("on");
            });
            swatches.add(sw);
        }

        Button save = new Button("Save", e -> {
            if (text.getValue().isBlank()) { Notification.show("Write something first"); return; }
            if (existing == null) notes.add(new NoteItem(text.getValue().trim(), picked[0], LocalDate.now(), false));
            else { existing.text = text.getValue().trim(); existing.color = picked[0]; notes.save(existing); }
            d.close();
            refresh();
        });
        save.addClassName("nei-btn-primary");
        d.add(new Div(text, swatches));
        d.getFooter().add(new Button("Cancel", e -> d.close()));
        if (existing != null) d.getFooter().add(new Button("Delete", e -> { notes.delete(existing); d.close(); refresh(); }));
        d.getFooter().add(save);
        d.open();
    }
}
