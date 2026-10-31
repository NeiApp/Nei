package com.projectii.Nei.ui.welcome;

import com.projectii.Nei.ui.model.AccountSettings;
import com.projectii.Nei.ui.service.SettingsUiService;
import com.projectii.Nei.ui.shared.Appearance;
import com.projectii.Nei.ui.shared.NeiView;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/** First-run screen: a planner page on a warm desk, with the cat as the hero drawing. */
@CssImport("./styles/base.css")
@CssImport("./styles/welcome.css")
@Route("welcome")   // << CHANGED: was @Route("welcome") — onboarding now owns the front door
@PageTitle("Welcome | Nei")
public class WelcomeView extends NeiView implements BeforeEnterObserver {
    private final SettingsUiService settings;

    public WelcomeView(SettingsUiService settings) {
        this.settings = settings;
        refresh();
        Appearance.apply(this, settings.device());
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (settings.account().userName != null) {
            event.forwardTo("calendar");
        }
    }

    @Override
    protected void refresh() {
        removeAll();
        add(buildWelcome());
    }

    private Component buildWelcome() {
        AccountSettings account = settings.account();

        Div cat = new Div();
        cat.addClassName("nei-cat");
        cat.getElement().setAttribute("role", "img");
        cat.getElement().setAttribute("aria-label", "A sleeping cat with a small ringing bell");

        Div eyebrow = new Div(new Span("WELCOME"));
        eyebrow.addClassName("nei-eyebrow");

        H1 hello = new H1("Hi, I'm Nei.");
        Paragraph copy = new Paragraph("I'll help you plan your day, track what you finish, and look back on it.");

        Div tabs = new Div(
                dayTab("08:00", "Plan the morning"),
                dayTab("09:30", "Deep work"),
                dayTab("14:00", "Write today's note"));
        tabs.addClassName("nei-daytabs");
        tabs.getElement().setAttribute("aria-hidden", "true");

        TextField name = new TextField("What should I call you?");
        name.setPlaceholder("Your name");
        name.setAutofocus(true);
        name.setMaxLength(40);
        name.setWidthFull();

        // was: RadioButtonGroup<String> week = ...  → now a segmented tab control
        Component week = weekStartControl(account);

        Runnable start = () -> {
            if (name.getValue().isBlank()) {
                name.setInvalid(true);
                name.setErrorMessage("Tell me what to call you");
                return;
            }
            account.userName = name.getValue().trim();
            // account.weekStart is already set by the segmented control on click
            settings.save();
            go("calendar");
        };
        name.addKeyPressListener(Key.ENTER, e -> start.run());

        Button goBtn = new Button("Get started", new Icon(VaadinIcon.ARROW_RIGHT), e -> start.run());
        goBtn.setIconAfterText(true);
        goBtn.addClassName("nei-btn-primary");
        goBtn.setWidthFull();

        Span hint = new Span("You can change all of this later in Settings.");
        hint.addClassName("nei-form-hint");

        Div sheet = new Div(cat, eyebrow, hello, copy, tabs, name, week, goBtn, hint);
        sheet.addClassName("nei-sheet");

        Div shell = new Div(sheet);
        shell.addClassName("nei-welcome");
        shell.addClassName("nei-desk");
        return shell;
    }

    /** Two mutually-exclusive pills in a track, bound to account.weekStart. */
    private Component weekStartControl(AccountSettings account) {
        Div label = new Div(new Span("Your week starts on"));
        label.addClassName("nei-segweek-label");

        Div track = new Div();
        track.addClassName("nei-segweek");
        track.getElement().setAttribute("role", "radiogroup");
        track.getElement().setAttribute("aria-label", "Your week starts on");

        final String[] days = {"Monday", "Sunday"};
        final Button[] opts = new Button[days.length];
        for (int i = 0; i < days.length; i++) {
            final int idx = i;
            Button b = new Button(days[i], e -> {
                account.weekStart = days[idx];
                for (int j = 0; j < opts.length; j++) {
                    boolean on = j == idx;
                    if (on) opts[j].addClassName("on"); else opts[j].removeClassName("on");
                    opts[j].getElement().setAttribute("aria-checked", String.valueOf(on));
                }
            });
            b.addClassName("nei-segopt");
            b.getElement().setAttribute("role", "radio");
            b.getElement().setAttribute("aria-checked", String.valueOf(days[i].equals(account.weekStart)));
            if (days[i].equals(account.weekStart)) b.addClassName("on");
            opts[idx] = b;
            track.add(b);
        }
        return new Div(label, track);
    }

    private Component dayTab(String time, String label) {
        Span t = new Span(time);
        t.addClassName("t");
        Span l = new Span(label);
        l.addClassName("l");
        Div tab = new Div(t, l);
        tab.addClassName("nei-tab");
        return tab;
    }
}