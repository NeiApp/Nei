package com.projectii.Nei.ui.settings;

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

@Route(value = "settings", layout = MainLayout.class)
@PageTitle("Settings | Nei")
public class SettingsView extends NeiView {
    private final SettingsUiService settings;
    private final UiState state;

    public SettingsView(SettingsUiService settings, UiState state) {
        this.settings = settings;
        this.state = state;
        refresh();
    }

    @Override
    protected void refresh() {
        removeAll();
        add(buildSettings());
    }

    private void applyAppearance() {
        Appearance.apply(this, settings.device());
    }

    private Component buildSettings() {
        // --- profile and time
        TextField name = new TextField();
        name.setValue(settings.account().userName);
        name.addValueChangeListener(e -> { if (!e.getValue().isBlank()) { settings.account().userName = e.getValue().trim(); settings.save(); } });
        ComboBox<String> zone = new ComboBox<>();
        zone.setItems(new ArrayList<>(new java.util.TreeSet<>(java.time.ZoneId.getAvailableZoneIds())));
        zone.setValue(settings.account().timezone);
        zone.addValueChangeListener(e -> { if (e.getValue() != null) { settings.account().timezone = e.getValue(); settings.save(); } });
        RadioButtonGroup<String> clock = new RadioButtonGroup<>();
        clock.setItems("24-hour", "12-hour");
        clock.setValue(settings.account().use24h ? "24-hour" : "12-hour");
        clock.addValueChangeListener(e -> { settings.account().use24h = "24-hour".equals(e.getValue()); settings.save(); });

        // --- appearance
        Div swatches = new Div();
        swatches.addClassName("nei-swatches");
        for (String c : Palette.ACCENTS) {
            Span dot = new Span();
            dot.addClassName("nei-cdot");
            dot.getStyle().set("background", c);
            if (c.equals(settings.device().accent)) dot.addClassName("on");
            dot.addClickListener(e -> {
                settings.device().accent = c;
                settings.save();
                applyAppearance();
                swatches.getChildren().forEach(x -> x.getElement().getClassList().remove("on"));
                dot.addClassName("on");
            });
            swatches.add(dot);
        }

        // --- desktop
        TextField shortcut = new TextField();
        shortcut.setValue(settings.device().quickAddShortcut);
        shortcut.addValueChangeListener(e -> { settings.device().quickAddShortcut = e.getValue(); settings.save(); });

        // --- data
        Button exportTasks = new Button("Export tasks", e -> Notification.show("Export arrives with the backend"));
        Button exportNotes = new Button("Export notes", e -> Notification.show("Export arrives with the backend"));
        Button backup = new Button("Back up", e -> Notification.show("Backup arrives with the backend"));
        Button wipe = new Button("Delete all data", e -> confirmWipe());
        wipe.addClassName("nei-danger");
        Div dataBtns = new Div(exportTasks, exportNotes, backup, wipe);
        dataBtns.addClassName("nei-set-btns");

        // --- sync placeholder
        RadioButtonGroup<String> mode = new RadioButtonGroup<>();
        mode.setItems("Local only", "Sync when online");
        mode.setValue("Local only");
        mode.setItemEnabledProvider(i -> i.equals("Local only"));
        Button syncNow = new Button("Sync now");
        syncNow.setEnabled(false);

        Div page = new Div(new H1("Settings"),
                setCard("Profile and time", "Syncs to phone",
                        setRow("Your name", "What Nei calls you", name),
                        setRow("Timezone", "Used for alarms and your day boundaries", zone),
                        setRow("Week starts on", "Calendar and weekly review", pick(settings.account().weekStart, v -> settings.account().weekStart = v, "Monday", "Sunday")),
                        setRow("Clock", null, clock)),
                setCard("Your day", "Syncs to phone",
                        setRow("Day starts", null, time(settings.account().workStart, v -> settings.account().workStart = v)),
                        setRow("Day ends", null, time(settings.account().workEnd, v -> settings.account().workEnd = v)),
                        setRow("Overload warning", "Nei warns you above this many open tasks in a day",
                                num(settings.account().maxTasksPerDay, 1, 30, v -> settings.account().maxTasksPerDay = v))),
                setCard("Reminders and alarms", "Syncs to phone",
                        setRow("Notifications", null, toggle(settings.account().notifications, v -> settings.account().notifications = v)),
                        setRow("Default alarm interval", "Minutes between reminders", num(settings.account().defaultAlarmMin, 5, 720, v -> settings.account().defaultAlarmMin = v)),
                        setRow("Snooze length", "Minutes", num(settings.account().snoozeMin, 1, 120, v -> settings.account().snoozeMin = v)),
                        setRow("Quiet hours from", "No alarms between these times", time(settings.account().quietFrom, v -> settings.account().quietFrom = v)),
                        setRow("Quiet hours until", null, time(settings.account().quietTo, v -> settings.account().quietTo = v)),
                        setRow("Ask when a task ends", "\"Done?\" prompt, then move to the next task", toggle(settings.account().askWhenDone, v -> settings.account().askWhenDone = v))),
                setCard("Reflection and reviews", "Syncs to phone",
                        setRow("Ask \"How was your day?\" at", null, time(settings.account().reflectionTime, v -> settings.account().reflectionTime = v)),
                        setRow("Weekly review day", null, pick(settings.account().reviewDay, v -> settings.account().reviewDay = v,
                                "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"))),
                setCard("Appearance", "This device",
                        setRow("Accent colour", null, swatches),
                        setRow("Reduce motion", "Turns off animations and pulsing",
                                toggle(settings.device().reduceMotion, v -> { settings.device().reduceMotion = v; applyAppearance(); }))),
                setCard("Desktop", "This device",
                        setRow("Launch at login", "Needs the desktop wrapper", toggle(settings.device().launchAtLogin, v -> settings.device().launchAtLogin = v)),
                        setRow("Keep running in the tray", "So alarms still fire when the window is closed", toggle(settings.device().minimizeToTray, v -> settings.device().minimizeToTray = v)),
                        setRow("Quick-add shortcut", "Needs the desktop wrapper", shortcut),
                        setRow("Alarm sound", null, pick(settings.device().sound, v -> settings.device().sound = v, "Soft chime", "Bell", "Pulse", "Silent"))),
                setCard("Your data", "This device",
                        setRow("Export, back up or reset", "Your data lives on this computer, so keep a backup", dataBtns)),
                setCard("Sync", "Coming soon",
                        setRow("Mode", "Phone companion and sync arrive in a later version", mode),
                        setRow("Last synced", "Never", syncNow)));
        page.addClassName("nei-settings");
        return page;
    }

    private Component setRow(String label, String hint, Component control) {
        Div text = new Div(new Span(label));
        text.addClassName("nei-set-text");
        if (hint != null) {
            Span h = new Span(hint);
            h.addClassName("nei-set-hint");
            text.add(h);
        }
        Div row = new Div(text, control);
        row.addClassName("nei-set-row");
        return row;
    }

    private Component setCard(String title, String tag, Component... rows) {
        Span chip = new Span(tag);
        chip.addClassNames("nei-set-tag", tag.equals("This device") ? "device" : tag.equals("Coming soon") ? "soon" : "sync");
        Div head = new Div(new H4(title), chip);
        head.addClassName("nei-set-head");
        Div card = new Div(head);
        card.addClassNames("nei-card", "nei-set-card");
        card.add(rows);
        return card;
    }

    private Checkbox toggle(boolean value, Consumer<Boolean> set) {
        Checkbox c = new Checkbox(value);
        c.addValueChangeListener(e -> { set.accept(e.getValue()); settings.save(); });
        return c;
    }

    private IntegerField num(int value, int min, int max, Consumer<Integer> set) {
        IntegerField f = new IntegerField();
        f.setMin(min);
        f.setMax(max);
        f.setValue(value);
        f.setStepButtonsVisible(true);
        f.setWidth("130px");
        f.addValueChangeListener(e -> { if (e.getValue() != null) { set.accept(e.getValue()); settings.save(); } });
        return f;
    }

    private TimePicker time(LocalTime value, Consumer<LocalTime> set) {
        TimePicker t = new TimePicker();
        t.setValue(value);
        t.setWidth("130px");
        t.addValueChangeListener(e -> { if (e.getValue() != null) { set.accept(e.getValue()); settings.save(); } });
        return t;
    }

    private Select<String> pick(String value, Consumer<String> set, String... items) {
        Select<String> sel = new Select<>();
        sel.setItems(items);
        sel.setValue(value);
        sel.setWidth("170px");
        sel.addValueChangeListener(e -> { if (e.getValue() != null) { set.accept(e.getValue()); settings.save(); } });
        return sel;
    }

    private void confirmWipe() {
        Dialog d = new Dialog();
        d.setHeaderTitle("Delete all data?");
        d.add(new Div("This removes every task and note on this computer. It can't be undone."));
        Button yes = new Button("Delete everything", e -> {
            settings.deleteAllData();
            state.selected = null;
            state.running = null;
            d.close();
            Notification.show("All data deleted");
            refresh();
        });
        yes.addClassName("nei-danger");
        d.getFooter().add(new Button("Cancel", e -> d.close()), yes);
        d.open();
    }
}
