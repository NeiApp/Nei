package com.projectii.Nei.ui.notes;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.projectii.Nei.ui.model.*;
import com.projectii.Nei.ui.service.*;
import com.projectii.Nei.ui.shared.*;
import com.projectii.Nei.ui.app.MainLayout;

@CssImport("./styles/components.css")
@CssImport("./styles/notes.css")
@Route(value = "notes", layout = MainLayout.class)
@PageTitle("Notes | Nei")
public class NotesView extends NeiView implements AfterNavigationObserver {
    private final TaskUiService tasks;
    private final NoteUiService notes;
    private final UiState state;
    private final NoteEditor editor;

    private String noteSearch = "";
    private String noteColorFilter = null;

    private final Div listContainer = new Div();
    private final List<Span> filterDots = new ArrayList<>();
    private final Map<Object, Span> snippetBySrc = new HashMap<>();  // in-place text updates
    private final Map<Object, Div> rowBySrc = new HashMap<>();       // highlight by reference

    public NotesView(TaskUiService tasks, NoteUiService notes, UiState state) {
        this.tasks = tasks;
        this.notes = notes;
        this.state = state;
        this.editor = new NoteEditor(notes, tasks, this::syncText, this::refreshList, () -> go("today"));
        buildShell();
        refreshList();
    }

    /** The list is the only thing that re-renders; the editor instance persists, so typing keeps focus. */
    @Override
    protected void refresh() {
        refreshList();
    }

    /**
     * Fired each time we navigate INTO /notes. If a task is the global selection
     * (set by Today's "Open Notes" or Calendar), open its note here, editable.
     */
    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        if (state.selected != null) {
            editor.loadTask(state.selected);
            refreshList();
        }
    }

    private void buildShell() {
        Button newBtn = new Button();
        newBtn.setText("+ New");
        newBtn.addClassName("nei-btn-primary");
        newBtn.addClassName("nei-note-newbtn");
        newBtn.addClickListener(e -> editor.newNote());

        TextField search = new TextField();
        search.addClassName("nei-note-search");
        search.setPlaceholder("Search notes");
        search.setPrefixComponent(new Icon(VaadinIcon.SEARCH));
        search.setValueChangeMode(ValueChangeMode.EAGER);
        search.addValueChangeListener(e -> {
            noteSearch = e.getValue() == null ? "" : e.getValue();
            refreshList();
        });

        Div dots = new Div();
        dots.addClassName("nei-note-dots");
        for (String c : Palette.NOTE_COLORS) {
            Span dot = new Span();
            dot.addClassName("nei-cdot");
            dot.getStyle().set("background", c);
            dot.getElement().addEventListener("click", ev -> {
                noteColorFilter = c.equals(noteColorFilter) ? null : c;
                refreshList();
            });
            filterDots.add(dot);
            dots.add(dot);
        }

        Div toolsRow = new Div(search, dots);
        toolsRow.addClassName("nei-note-toolsrow");
        Div headRow = new Div(new H1("Notes"), newBtn);
        headRow.addClassName("nei-note-headrow");
        Div top = new Div(headRow, toolsRow);
        top.addClassName("nei-note-top");

        listContainer.addClassName("nei-note-list");
        Div listCol = new Div(top, listContainer);
        listCol.addClassName("nei-note-listcol");

        Div page = new Div(listCol, editor);
        page.addClassName("nei-notes");
        removeAll();
        add(page);
    }

    /** Cheap path while typing: update just the open row's snippet, no rebuild, no focus loss. */
    private void syncText() {
        Object src = editor.currentNote() != null ? editor.currentNote() : editor.currentTask();
        Span s = src == null ? null : snippetBySrc.get(src);
        if (s != null) s.setText(snippet(textOf(src)));
    }

    private void refreshList() {
        listContainer.removeAll();
        snippetBySrc.clear();
        rowBySrc.clear();
        markFilterDots();

        String q = noteSearch.toLowerCase();
        List<Row> rows = new ArrayList<>();

        for (NoteItem n : notes.all()) {
            rows.add(Row.ofNote(n));
        }

        for (TaskItem t : tasks.all()) {
            boolean blank = t.note == null || t.note.isBlank();
            if (blank && t != state.selected) continue;
            rows.add(Row.ofTask(t, "Work".equals(t.list) ? Palette.NOTE_COLORS[3] : Palette.NOTE_COLORS[0]));
        }

        rows.sort(Comparator.comparing((Row r) -> r.date, Comparator.nullsFirst(Comparator.naturalOrder())).reversed());

        rows.stream()
                .filter(r -> noteColorFilter == null || r.color.equals(noteColorFilter))
                .filter(r -> q.isBlank() || r.text.toLowerCase().contains(q)
                        || (r.title != null && r.title.toLowerCase().contains(q)))
                .forEach(r -> listContainer.add(row(r)));

        if (rows.isEmpty() || listContainer.getChildren().findAny().isEmpty()) {
            Div empty = new Div("No notes yet. Tap + New to write about your day.");
            empty.addClassName("nei-empty");
            listContainer.add(empty);
        }

        highlight();
    }

    private Component row(Row r) {
        Span snippet = new Span(snippet(r.text));
        snippet.addClassName("nei-note-snippet");

        Span date = new Span(r.date == null ? "" : r.date.getMonth().toString().substring(0, 3) + " " + r.date.getDayOfMonth());
        date.addClassName("nei-note-date");

        Div top = new Div(snippet, date);
        top.addClassName("nei-note-rowtop");

        Div sub = new Div();
        sub.addClassName("nei-note-rowsub");

        if (r.task != null) {
            Span tag = new Span("Task");
            tag.addClassName("nei-note-tasktag");

            Span title = new Span(r.title == null ? "" : r.title);
            title.addClassName("nei-note-sub");

            sub.add(tag, title);
        } else {
            Span preview = new Span(secondLine(r.text));
            preview.addClassName("nei-note-sub");
            sub.add(preview);

            if (r.starred) {
                Span st = new Span(new Icon(VaadinIcon.STAR));
                st.addClassName("nei-note-starspan");
                sub.add(st);
            }
        }

        Div row = new Div(top, sub);
        row.addClassName("nei-note-row");
        row.getStyle().set("--note-tint", r.color);

        row.addClickListener(e -> {
            if (r.note != null) {
                state.selected = null;
                editor.loadNote(r.note);
            } else {
                state.selected = r.task;
                editor.loadTask(r.task);
            }
            highlight();
        });

        Object src = r.note != null ? r.note : (Object) r.task;
        snippetBySrc.put(src, snippet);
        rowBySrc.put(src, row);
        return row;
    }

    private void highlight() {
        Object cur = editor.currentNote() != null ? editor.currentNote() : editor.currentTask();
        rowBySrc.forEach((src, row) -> {
            if (src == cur) row.addClassName("sel");
            else row.removeClassName("sel");
        });
    }

    private void markFilterDots() {
        for (int i = 0; i < filterDots.size(); i++) {
            Span d = filterDots.get(i);
            if (Palette.NOTE_COLORS[i].equals(noteColorFilter)) d.addClassName("on");
            else d.removeClassName("on");
        }
    }

    private String textOf(Object src) {
        if (src instanceof NoteItem n) return n.text == null ? "" : n.text;
        if (src instanceof TaskItem t) return t.note == null ? "" : t.note;
        return "";
    }

    private String snippet(String text) {
        String t = text == null ? "" : text.strip();
        if (t.isEmpty()) return "Untitled";
        int nl = t.indexOf('\n');
        if (nl > 0) t = t.substring(0, nl);
        return t.length() > 64 ? t.substring(0, 64) + "…" : t;
    }

    private String secondLine(String text) {
        String t = text == null ? "" : text.strip();
        int nl = t.indexOf('\n');
        return nl < 0 ? "" : t.substring(nl + 1).strip();
    }

    /** One flattened row for either a real note or a task-derived note. */
    private record Row(String text, String title, String color, LocalDate date,
                       boolean starred, NoteItem note, TaskItem task) {
        static Row ofNote(NoteItem n) {
            return new Row(n.text, null, n.color, n.date, n.starred, n, null);
        }

        static Row ofTask(TaskItem t, String color) {
            return new Row(t.note, t.title, color, t.due, false, null, t);
        }
    }
}