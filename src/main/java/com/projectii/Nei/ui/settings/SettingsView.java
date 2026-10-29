package com.projectii.Nei.ui.settings;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.timepicker.TimePicker;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.projectii.Nei.ui.model.*;
import com.projectii.Nei.ui.service.*;
import com.projectii.Nei.ui.shared.*;
import com.projectii.Nei.ui.app.MainLayout;

@CssImport("./styles/components.css")
@CssImport("./styles/settings.css")
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
        applyAppearance(settings.device());
    }

    private void applyAppearance(DeviceSettings dev) {
        String mode = dev.themeMode == null ? "light" : dev.themeMode.toLowerCase();
        String accent = dev.accent == null ? "#f7c531" : dev.accent;

        getUI().ifPresent(ui -> ui.getPage().executeJs(
                "const el = document.documentElement;"
                        + "el.classList.remove('theme-dark','theme-warm','sidebar-transparent','nei-reduce-motion');"
                        + "if ($0 !== 'light') el.classList.add('theme-' + $0);"
                        + "if ($1) el.classList.add('sidebar-transparent');"
                        + "if ($2) el.classList.add('nei-reduce-motion');"
                        + "el.style.setProperty('--nei-accent', $3);",
                mode, dev.sidebarTransparent, dev.reduceMotion, accent));
    }

    private Component buildSettings() {
        // --- profile and time
        TextField name = new TextField();
        name.setValue(settings.account().userName);
        name.addValueChangeListener(e -> {
            if (!e.getValue().isBlank()) {
                settings.account().userName = e.getValue().trim();
                settings.save();
            }
        });

        ComboBox<String> zone = new ComboBox<>();
        zone.setItems(new ArrayList<>(new java.util.TreeSet<>(java.time.ZoneId.getAvailableZoneIds())));
        zone.setValue(settings.account().timezone);
        zone.addValueChangeListener(e -> {
            if (e.getValue() != null) {
                settings.account().timezone = e.getValue();
                settings.save();
            }
        });

        Component weekStart = segmented(
                settings.account().weekStart,
                v -> settings.account().weekStart = v,
                "Monday", "Sunday");

        Component clock = segmented(
                settings.account().use24h ? "24-hour" : "12-hour",
                v -> settings.account().use24h = "24-hour".equals(v),
                "24-hour", "12-hour");

        // --- appearance: accent swatches
        Div swatches = new Div();
        swatches.addClassName("nei-swatches");

        String[] accentColors = Palette.ACCENTS;
        NativeButton[] swatchButtons = new NativeButton[accentColors.length];

        for (int i = 0; i < accentColors.length; i++) {
            final String color = accentColors[i];
            NativeButton swatch = new NativeButton();
            swatch.addClassName("nei-swatch");
            swatch.getElement().getStyle().set("background", color);
            swatch.getElement().setAttribute("title", color);
            swatch.getElement().setAttribute("aria-label", "Accent colour " + color);

            boolean selected = color.equals(settings.device().accent);
            if (selected) swatch.addClassName("on");
            swatch.getElement().setAttribute("aria-pressed", String.valueOf(selected));

            swatch.addClickListener(e -> setAccent(color, swatchButtons, swatch));

            swatchButtons[i] = swatch;
            swatches.add(swatch);
        }

        // --- appearance: sidebar transparency toggle
        Component sidebar = toggle(
                "Sidebar transparency",
                settings.device().sidebarTransparent,
                v -> {
                    DeviceSettings dev = settings.device();
                    dev.sidebarTransparent = v;
                    applyAppearance(dev);
                });

        // --- appearance: reduce motion toggle
        Component reduce = toggle(
                "Reduce motion",
                settings.device().reduceMotion,
                v -> {
                    DeviceSettings dev = settings.device();
                    dev.reduceMotion = v;
                    applyAppearance(dev);
                });

        // --- appearance: theme cards
        Div themeCards = buildThemeCards();

        // --- desktop
        TextField shortcut = new TextField();
        shortcut.setValue(settings.device().quickAddShortcut);
        shortcut.addValueChangeListener(e -> {
            settings.device().quickAddShortcut = e.getValue();
            settings.save();
        });

        // --- data
        Button exportTasks = new Button("Export tasks", e -> Notification.show("Export arrives with the backend"));
        Button exportNotes = new Button("Export notes", e -> Notification.show("Export arrives with the backend"));
        Button backup = new Button("Back up", e -> Notification.show("Backup arrives with the backend"));
        Button wipe = new Button("Delete all data", e -> confirmWipe());

        exportTasks.addClassName("nei-btn-ghost");
        exportNotes.addClassName("nei-btn-ghost");
        backup.addClassName("nei-btn-ghost");
        wipe.addClassName("nei-danger");

        Div dataBtns = new Div(exportTasks, exportNotes, backup, wipe);
        dataBtns.addClassName("nei-set-btns");

        // --- sync placeholder
        RadioButtonGroup<String> mode = new RadioButtonGroup<>();
        mode.setItems("Local only", "Sync when online");
        mode.setValue("Local only");
        mode.setItemEnabledProvider(i -> i.equals("Local only"));

        Button syncNow = new Button("Sync now");
        syncNow.addClassName("nei-btn-ghost");
        syncNow.setEnabled(false);

        Div page = new Div(new H1("Settings"),
                setCard("Profile and time", "Syncs to phone",
                        setRow("Your name", "What Nei calls you", name),
                        setRow("Timezone", "Used for alarms and your day boundaries", zone),
                        setRow("Week starts on", "Calendar and weekly review", weekStart),
                        setRow("Clock", null, clock)),

                setCard("Your day", "Syncs to phone",
                        setRow("Day starts", null, time(settings.account().workStart, v -> settings.account().workStart = v)),
                        setRow("Day ends", null, time(settings.account().workEnd, v -> settings.account().workEnd = v)),
                        setRow("Overload warning", "Nei warns you above this many open tasks in a day",
                                num(settings.account().maxTasksPerDay, 1, 30, v -> settings.account().maxTasksPerDay = v))),

                setCard("Reminders and alarms", "Syncs to phone",
                        setRow("Notifications", null,
                                toggle("Notifications", settings.account().notifications,
                                        v -> settings.account().notifications = v)),
                        setRow("Default alarm interval", "Minutes between reminders",
                                num(settings.account().defaultAlarmMin, 5, 720, v -> settings.account().defaultAlarmMin = v)),
                        setRow("Snooze length", "Minutes",
                                num(settings.account().snoozeMin, 1, 120, v -> settings.account().snoozeMin = v)),
                        setRow("Quiet hours from", "No alarms between these times",
                                time(settings.account().quietFrom, v -> settings.account().quietFrom = v)),
                        setRow("Quiet hours until", null,
                                time(settings.account().quietTo, v -> settings.account().quietTo = v)),
                        setRow("Ask when a task ends", "\"Done?\" prompt, then move to the next task",
                                toggle("Ask when a task ends", settings.account().askWhenDone,
                                        v -> settings.account().askWhenDone = v))),

                setCard("Reflection and reviews", "Syncs to phone",
                        setRow("Ask \"How was your day?\" at", null,
                                time(settings.account().reflectionTime, v -> settings.account().reflectionTime = v)),
                        setRow("Weekly review day", null,
                                pick(settings.account().reviewDay, v -> settings.account().reviewDay = v,
                                        "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"))),

                setCard("Appearance", "This device", "nei-appearance-card",
                        setRow("Theme", "Tap a look to apply it instantly", themeCards, "nei-set-row-stack"),
                        setRow("Accent colour", "Highlights, active dock orb, focus rings", swatches, "nei-set-row-stack"),
                        setRow("Sidebar", "Make the dock transparent / frosted", sidebar),
                        setRow("Reduce motion", "Turns off animations and pulsing", reduce)),

                setCard("Desktop", "This device",
                        setRow("Launch at login", "Needs the desktop wrapper",
                                toggle("Launch at login", settings.device().launchAtLogin,
                                        v -> settings.device().launchAtLogin = v)),
                        setRow("Keep running in the tray", "So alarms still fire when the window is closed",
                                toggle("Keep running in the tray", settings.device().minimizeToTray,
                                        v -> settings.device().minimizeToTray = v)),
                        setRow("Quick-add shortcut", "Needs the desktop wrapper", shortcut),
                        setRow("Alarm sound", null,
                                pick(settings.device().sound, v -> settings.device().sound = v,
                                        "Soft chime", "Bell", "Pulse", "Silent"))),

                setCard("Your data", "This device",
                        setRow("Export, back up or reset", "Your data lives on this computer, so keep a backup", dataBtns)),

                setCard("Sync", "Coming soon",
                        setRow("Mode", "Phone companion and sync arrive in a later version", mode),
                        setRow("Last synced", "Never", syncNow)));

        page.addClassName("nei-settings");
        return page;
    }

    // ---- theme picker ----

    private Div buildThemeCards() {
        String[] modes = {"light", "dark", "warm"};
        String[] labels = {"Daylight", "Midnight", "Amber"};

        Div picker = new Div();
        picker.addClassName("nei-theme-picker");

        NativeButton[] cards = new NativeButton[modes.length];

        for (int i = 0; i < modes.length; i++) {
            final String mode = modes[i];
            boolean selected = mode.equals(settings.device().themeMode);

            NativeButton card = new NativeButton();
            card.addClassName("nei-theme-card");
            card.addClassName("tc-" + mode);
            card.getElement().setAttribute("title", labels[i]);
            card.getElement().setAttribute("aria-label", labels[i] + " theme");
            card.getElement().setAttribute("aria-pressed", String.valueOf(selected));

            if (selected) card.addClassName("sel");

            card.add(miniWindow());

            Span name = new Span(labels[i]);
            name.addClassName("nei-theme-name");
            card.add(name);

            card.addClickListener(e -> setTheme(mode, cards, card));

            cards[i] = card;
            picker.add(card);
        }

        return picker;
    }

    private void setTheme(String mode, NativeButton[] cards, NativeButton chosen) {
        DeviceSettings dev = settings.device();
        dev.themeMode = mode;
        settings.save();

        for (NativeButton card : cards) {
            card.removeClassName("sel");
            card.getElement().setAttribute("aria-pressed", "false");
        }

        chosen.addClassName("sel");
        chosen.getElement().setAttribute("aria-pressed", "true");

        applyAppearance(dev);
    }

    private void setAccent(String color, NativeButton[] swatches, NativeButton chosen) {
        DeviceSettings dev = settings.device();
        dev.accent = color;
        settings.save();

        for (NativeButton swatch : swatches) {
            swatch.removeClassName("on");
            swatch.getElement().setAttribute("aria-pressed", "false");
        }

        chosen.addClassName("on");
        chosen.getElement().setAttribute("aria-pressed", "true");

        applyAppearance(dev);
    }

    private Component miniWindow() {
        Div w = new Div();
        w.addClassName("mk-win");

        Div bar = new Div();
        bar.addClassName("mk-bar");
        bar.add(new Span(), new Span(), new Span());

        Div row = new Div();
        row.addClassName("mk-row");

        Div side = new Div();
        side.addClassName("mk-side");
        side.add(new Span(), new Span(), new Span());

        Div main = new Div();
        main.addClassName("mk-main");
        main.add(new Div(), new Div());

        row.add(side, main);

        Div dot = new Div();
        dot.addClassName("mk-dot");

        w.add(bar, row, dot);
        return w;
    }

    // ---- segmented control ----

    private Component segmented(String value, Consumer<String> set, String... items) {
        Div track = new Div();
        track.addClassName("nei-seg");
        track.getElement().setAttribute("role", "radiogroup");

        NativeButton[] buttons = new NativeButton[items.length];

        for (int i = 0; i < items.length; i++) {
            final String item = items[i];

            NativeButton button = new NativeButton(item);
            button.addClassName("nei-seg-opt");
            button.getElement().setAttribute("role", "radio");

            boolean selected = item.equals(value);
            if (selected) button.addClassName("on");
            button.getElement().setAttribute("aria-checked", String.valueOf(selected));

            button.addClickListener(e -> setSegment(buttons, button, item, set));

            buttons[i] = button;
            track.add(button);
        }

        return track;
    }

    private void setSegment(NativeButton[] buttons, NativeButton chosen, String value, Consumer<String> set) {
        for (NativeButton button : buttons) {
            button.removeClassName("on");
            button.getElement().setAttribute("aria-checked", "false");
        }

        chosen.addClassName("on");
        chosen.getElement().setAttribute("aria-checked", "true");

        set.accept(value);
        settings.save();
    }

    // ---- toggle switch ----

    private Component toggle(String label, boolean value, Consumer<Boolean> set) {
        NativeButton toggle = new NativeButton();
        toggle.addClassName("nei-toggle");

        if (value) toggle.addClassName("on");

        toggle.getElement().setAttribute("role", "switch");
        toggle.getElement().setAttribute("aria-checked", String.valueOf(value));
        toggle.getElement().setAttribute("aria-label", label);

        Span knob = new Span();
        knob.addClassName("nei-toggle-knob");
        toggle.add(knob);

        toggle.addClickListener(e -> flipToggle(toggle, set));

        return toggle;
    }

    private void flipToggle(NativeButton toggle, Consumer<Boolean> set) {
        boolean on = !toggle.getElement().getClassList().contains("on");

        if (on) toggle.addClassName("on");
        else toggle.removeClassName("on");

        toggle.getElement().setAttribute("aria-checked", String.valueOf(on));

        set.accept(on);
        settings.save();
    }

    // ---- existing builders ----

    private Component setRow(String label, String hint, Component control) {
        return setRow(label, hint, control, null);
    }

    private Component setRow(String label, String hint, Component control, String extraClass) {
        Div text = new Div(new Span(label));
        text.addClassName("nei-set-text");

        if (hint != null) {
            Span h = new Span(hint);
            h.addClassName("nei-set-hint");
            text.add(h);
        }

        Div row = new Div(text, control);
        row.addClassName("nei-set-row");

        if (extraClass != null) row.addClassName(extraClass);

        return row;
    }

    private Component setCard(String title, String tag, Component... rows) {
        return setCard(title, tag, null, rows);
    }

    private Component setCard(String title, String tag, String extraClass, Component... rows) {
        Span chip = new Span(tag);
        chip.addClassNames("nei-set-tag",
                tag.equals("This device") ? "device" :
                        tag.equals("Coming soon") ? "soon" : "sync");

        Div head = new Div(new H4(title), chip);
        head.addClassName("nei-set-head");

        Div card = new Div(head);
        card.addClassNames("nei-card", "nei-set-card");

        if (extraClass != null) card.addClassName(extraClass);

        card.add(rows);
        return card;
    }

    private IntegerField num(int value, int min, int max, Consumer<Integer> set) {
        IntegerField f = new IntegerField();
        f.setMin(min);
        f.setMax(max);
        f.setValue(value);
        f.setStepButtonsVisible(true);
        f.setWidth("140px");
        f.addValueChangeListener(e -> {
            if (e.getValue() != null) {
                set.accept(e.getValue());
                settings.save();
            }
        });
        return f;
    }

    private TimePicker time(LocalTime value, Consumer<LocalTime> set) {
        TimePicker t = new TimePicker();
        t.setValue(value);
        t.setWidth("150px");
        t.addValueChangeListener(e -> {
            if (e.getValue() != null) {
                set.accept(e.getValue());
                settings.save();
            }
        });
        return t;
    }

    private Select<String> pick(String value, Consumer<String> set, String... items) {
        Select<String> sel = new Select<>();
        sel.setItems(items);
        sel.setValue(value);
        sel.setWidth("190px");
        sel.addValueChangeListener(e -> {
            if (e.getValue() != null) {
                set.accept(e.getValue());
                settings.save();
            }
        });
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
            applyAppearance();
        });

        yes.addClassName("nei-danger");
        d.getFooter().add(new Button("Cancel", e -> d.close()), yes);
        d.open();
    }
}