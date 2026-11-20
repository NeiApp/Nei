package com.projectii.Nei.ui.shared;

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

/** The note and alarm dialogs for a task. Used by Today and by the task-note cards on Notes. */
public final class TaskDialogs {
    private TaskDialogs() {}

    public static void openNote(TaskItem t, TaskUiService tasks, Runnable after) {
        Dialog d = new Dialog();
        d.setHeaderTitle("Note: " + t.title);
        TextArea note = new TextArea("What did you learn or get done?");
        note.setValue(t.note);
        note.setWidth("380px");
        note.setMaxHeight("300px");
        note.setMaxLength(2000);
        Button save = new Button("Save note", e -> {
            t.note = note.getValue();
            tasks.save(t); // MOCK: taskService.addNoteToTask(id, note)
            Notification.show("Note saved");
            d.close();
            after.run();
        });
        save.addClassName("nei-btn-primary");
        d.add(note);
        d.getFooter().add(new Button("Cancel", e -> d.close()), save);
        d.open();
    }

    public static void openAlarm(TaskItem t, TaskUiService tasks, SettingsUiService settings, Runnable after) {
        Dialog d = new Dialog();
        d.setHeaderTitle("Alarm: " + t.title);
        IntegerField every = new IntegerField("Remind every (minutes)");
        every.setValue(t.alarm != null ? t.alarm : settings.account().defaultAlarmMin);
        every.setMin(5);
        every.setStepButtonsVisible(true);
        Button save = new Button("Attach alarm", e -> {
            t.alarm = every.getValue();
            tasks.save(t); // MOCK: taskService.createAlarmReminder(id, request)
            Notification.show("Alarm attached");
            d.close();
            after.run();
        });
        save.addClassName("nei-btn-primary");
        d.add(every);
        d.getFooter().add(new Button("Cancel", e -> d.close()), save);
        if (t.alarm != null) {
            Button remove = new Button("Remove", e -> { t.alarm = null; tasks.save(t); d.close(); after.run(); });
            d.getFooter().add(remove);
        }
        d.open();
    }
}
