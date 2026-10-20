package com.projectii.Nei.ui.shared;

import com.projectii.Nei.ui.model.TaskItem;
import org.springframework.stereotype.Component;

/** Navigation state shared between screens (single-user desktop app, so one instance is fine). */
@Component
public class UiState {
    public TaskItem selected;     // task open in the Today detail panel
    public String listFilter;     // null = all lists
    public TaskItem running;      // task currently running in the Schedule flow

    public void reset() {
        selected = null;
        listFilter = null;
        running = null;
    }
}
