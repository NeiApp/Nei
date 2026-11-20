package com.projectii.Nei.ui.model;

/** UI view-model for a subtask. Map it from your SubTask entity later. */
public class SubItem {
    public String title;
    public boolean done;

    public SubItem(String title, boolean done) {
        this.title = title;
        this.done = done;
    }
}
