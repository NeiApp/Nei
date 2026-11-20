package com.projectii.Nei.ui.service;

import com.projectii.Nei.ui.model.NoteItem;
import java.util.List;

/** What the Notes and Calendar screens need. Backend today: nothing (only Task.note and DayLog.reflection exist). */
public interface NoteUiService {
    List<NoteItem> all();
    void add(NoteItem note);
    void save(NoteItem note);
    void delete(NoteItem note);
}
