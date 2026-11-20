package com.projectii.Nei.ui.model;

import java.time.LocalDate;
import java.util.UUID;

/** NEW: a standalone note. The backend has no Note entity yet. */
public class NoteItem {
    public final String id = UUID.randomUUID().toString();
    public String text, color;
    public LocalDate date;
    public boolean starred;

    public NoteItem(String text, String color, LocalDate date, boolean starred) {
        this.text = text;
        this.color = color;
        this.date = date;
        this.starred = starred;
    }
}
