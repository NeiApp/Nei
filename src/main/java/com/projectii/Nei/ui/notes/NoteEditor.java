package com.projectii.Nei.ui.notes;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextAreaVariant;
import com.vaadin.flow.data.value.ValueChangeMode;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.projectii.Nei.ui.model.*;
import com.projectii.Nei.ui.service.*;
import com.projectii.Nei.ui.shared.*;

/**
 * The right-hand writing surface of the Notes board. A persistent component (NOT a route):
 * NotesView creates ONE of these and calls loadNote()/loadTask()/newNote()/clear() to point it
 * at a different note, so the text you're typing is never torn down by a list re-render.
 * Saves go straight through NoteUiService or TaskUiService; it tells NotesView to refresh the
 * list via callbacks (onTextChange = cheap in-place row update, onStructuralChange = rebuild).
 */
public class NoteEditor extends Div {

    private final NoteUiService notes;
    private final TaskUiService tasks;
    private final Runnable onTextChange;
    private final Runnable onStructuralChange;
    private final Runnable onOpenTask;

    private final TextArea body = new TextArea();
    private final Span dateLbl = new Span();
    private final Button starBtn = new Button();
    private final Button deleteBtn = new Button();
    private final Button backBtn = new Button();
    private final Div banner = new Div();
    private final Div swatchRow = new Div();
    private final Div emptyState = new Div();
    private final List<Span> swatchDots = new ArrayList<>();

    private NoteItem current;          // non-null => editable real note
    private TaskItem currentTask;      // non-null => editable task-derived note
    private boolean loading;           // guards programmatic setValue from re-saving

    public NoteEditor(NoteUiService notes,
                      TaskUiService tasks,
                      Runnable onTextChange,
                      Runnable onStructuralChange,
                      Runnable onOpenTask) {
        this.notes = notes;
        this.tasks = tasks;
        this.onTextChange = onTextChange;
        this.onStructuralChange = onStructuralChange;
        this.onOpenTask = onOpenTask;
        addClassName("nei-note-editor");
        buildDom();
        showEmpty();
    }

    private void buildDom() {
        backBtn.addClassName("nei-round");
        backBtn.addClassName("nei-ed-back");
        backBtn.setIcon(new Icon(VaadinIcon.ARROW_LEFT));
        backBtn.getElement().setAttribute("aria-label", "Back to list");
        backBtn.addClickListener(e -> {
            clear();
            onStructuralChange.run();
        });

        dateLbl.addClassName("nei-ed-date");

        starBtn.addClassName("nei-round");
        starBtn.addClassName("nei-ed-star");
        starBtn.setIcon(new Icon(VaadinIcon.STAR_O));
        starBtn.getElement().setAttribute("aria-label", "Favourite");
        starBtn.addClickListener(e -> toggleStar());

        deleteBtn.addClassName("nei-round");
        deleteBtn.addClassName("nei-ed-del");
        deleteBtn.setIcon(new Icon(VaadinIcon.TRASH));
        deleteBtn.getElement().setAttribute("aria-label", "Delete note");
        deleteBtn.addClickListener(e -> confirmDelete());

        Div head = new Div(backBtn, dateLbl, starBtn, deleteBtn);
        head.addClassName("nei-ed-head");

        banner.addClassName("nei-ed-banner");
        banner.setVisible(false);

        body.setPlaceholder("Start writing…");
        body.setValueChangeMode(ValueChangeMode.LAZY);   // fires on a pause, not per keystroke
        body.setValueChangeTimeout(350);
        body.setSizeFull();
        body.addClassName("nei-ed-body");
        body.addThemeVariants(TextAreaVariant.LUMO_SMALL);
        body.addValueChangeListener(e -> {
            if (loading) return;

            if (currentTask != null) {
                currentTask.note = e.getValue();
                tasks.save(currentTask);
                onTextChange.run();
                return;
            }

            if (current != null) {
                current.text = e.getValue();
                notes.save(current);
                onTextChange.run();
            }
        });

        swatchRow.addClassName("nei-swatches");
        swatchRow.addClassName("nei-ed-swatches");
        for (String c : Palette.NOTE_COLORS) {
            Span dot = new Span();
            dot.addClassName("nei-cdot");
            dot.getStyle().set("background", c);
            dot.getElement().setAttribute("title", "Note colour");
            dot.getElement().addEventListener("click", ev -> pickColor(c));
            swatchDots.add(dot);
            swatchRow.add(dot);
        }
        Div foot = new Div(swatchRow);
        foot.addClassName("nei-ed-foot");

        Span ec = new Span();
        ec.addClassName("nei-ed-empty-cat");
        emptyState.addClassName("nei-ed-empty");
        emptyState.add(ec, new Span("Select a note, or start a new one."));

        add(head, banner, body, foot, emptyState);
    }

    // ---- public API used by NotesView ----

    public void loadNote(NoteItem n) {
        loading = true;
        current = n;
        currentTask = null;
        body.setReadOnly(false);
        body.setValue(n.text == null ? "" : n.text);
        dateLbl.setText(fmt(n.date));
        starBtn.setVisible(true);
        deleteBtn.setVisible(true);
        swatchRow.setVisible(true);
        banner.setVisible(false);
        markStar(n.starred);
        markSwatch(n.color);
        tint(n.color);
        showEditor();
        loading = false;
    }

    public void loadTask(TaskItem t) {
        loading = true;
        current = null;
        currentTask = t;
        body.setReadOnly(false);
        body.setValue(t.note == null ? "" : t.note);
        dateLbl.setText(t.due == null ? "" : fmt(t.due));
        starBtn.setVisible(false);
        deleteBtn.setVisible(false);
        swatchRow.setVisible(false);

        String color = "Work".equals(t.list) ? Palette.NOTE_COLORS[3] : Palette.NOTE_COLORS[0];

        banner.removeAll();
        banner.add(new Span("Linked to: " + (t.title == null ? "task" : t.title)));
        banner.add(openTodayBtn());
        banner.setVisible(true);

        tint(color);
        showEditor();
        loading = false;
    }

    /** Creates a blank note, persists it, loads it, focuses the body. Returns it so the list can select it. */
    public NoteItem newNote() {
        NoteItem n = new NoteItem("", Palette.NOTE_COLORS[0], LocalDate.now(), false);
        notes.add(n);
        loadNote(n);
        body.focus();
        onStructuralChange.run();
        return n;
    }

    public void clear() {
        current = null;
        currentTask = null;
        showEmpty();
    }

    public NoteItem currentNote() {
        return current;
    }

    public TaskItem currentTask() {
        return currentTask;
    }

    // ---- internals ----

    private Button openTodayBtn() {
        Button b = new Button();
        b.setText("Open Today");
        b.addClassName("nei-ed-bannerbtn");
        b.addClickListener(e -> onOpenTask.run());
        return b;
    }

    private void pickColor(String c) {
        if (loading || current == null) return;
        current.color = c;
        notes.save(current);
        markSwatch(c);
        tint(c);
        onStructuralChange.run();
    }

    private void toggleStar() {
        if (loading || current == null) return;
        current.starred = !current.starred;
        notes.save(current);
        markStar(current.starred);
        onStructuralChange.run();
    }

    private void confirmDelete() {
        if (current == null) return;

        Dialog d = new Dialog();
        d.setHeaderTitle("Delete this note?");
        d.add(new Div("This can't be undone."));

        Button yes = new Button();
        yes.setText("Delete");
        yes.addClassName("nei-danger");
        yes.addClickListener(e -> {
            notes.delete(current);
            clear();
            d.close();
            onStructuralChange.run();
        });

        Button cancel = new Button();
        cancel.setText("Cancel");
        cancel.addClickListener(e -> d.close());

        d.getFooter().add(cancel);
        d.getFooter().add(yes);
        d.open();
    }

    private void markStar(boolean on) {
        starBtn.setIcon(new Icon(on ? VaadinIcon.STAR : VaadinIcon.STAR_O));
        if (on) starBtn.addClassName("star-on");
        else starBtn.removeClassName("star-on");
    }

    private void markSwatch(String color) {
        for (int i = 0; i < swatchDots.size(); i++) {
            Span d = swatchDots.get(i);
            if (Palette.NOTE_COLORS[i].equals(color)) d.addClassName("on");
            else d.removeClassName("on");
        }
    }

    private void tint(String color) {
        getStyle().set("--note-tint", color == null ? "transparent" : color);
    }

    private void showEditor() {
        emptyState.addClassName("is-hidden");
        body.setVisible(true);
    }

    private void showEmpty() {
        emptyState.removeClassName("is-hidden");
        body.setVisible(false);
        banner.setVisible(false);
        tint("transparent");
    }

    private String fmt(LocalDate d) {
        return d == null ? "" : d.format(DateTimeFormatter.ofPattern("EEEE, MMM d yyyy"));
    }
}