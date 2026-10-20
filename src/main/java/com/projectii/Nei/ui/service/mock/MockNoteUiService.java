package com.projectii.Nei.ui.service.mock;

import com.projectii.Nei.ui.model.NoteItem;
import com.projectii.Nei.ui.service.NoteUiService;
import com.projectii.Nei.ui.shared.Palette;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** MOCK: in-memory notes. Replace with a Note entity, repository and real service. */
@Service
public class MockNoteUiService implements NoteUiService {
    private final List<NoteItem> notes = new ArrayList<>();

    @PostConstruct
    void seed() {
        LocalDate d = LocalDate.now();
        String[] c = Palette.NOTE_COLORS;
        notes.add(new NoteItem("This is my first Nei note. I'll write how the day went here.", c[0], d, false));
        notes.add(new NoteItem("Mornings work best for deep work. Move calls after lunch.", c[1], d.minusDays(1), true));
        notes.add(new NoteItem("Too many tasks on Tuesday. Cap the day at 5.", c[2], d.minusDays(3), false));
        notes.add(new NoteItem("Reading 20 pages before bed felt good.", c[3], d.minusDays(5), false));
        notes.add(new NoteItem("Ideas for the weekend plan.", c[4], d.minusDays(6), false));
    }

    @Override public List<NoteItem> all() { return notes; }
    @Override public void add(NoteItem note) { notes.add(0, note); }
    @Override public void save(NoteItem note) { /* mock: object already changed */ }
    @Override public void delete(NoteItem note) { notes.remove(note); }
}
