package com.projectii.Nei.ui.welcome;

import com.projectii.Nei.ui.app.MainLayout;
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
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/** First-run screen: a small desktop-style window with a preview on the left and the setup on the right. */
@CssImport("./styles/nei.css")
@Route("welcome")
@PageTitle("Welcome | Nei")
public class WelcomeView extends NeiView implements BeforeEnterObserver {
    private final SettingsUiService settings;

    public WelcomeView(SettingsUiService settings) {
        this.settings = settings;
        refresh();
        Appearance.apply(this, settings.device());
    }

    /** Already introduced yourself? Skip the gate. */
    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (settings.account().userName != null) event.forwardTo("today");
    }

    @Override
    protected void refresh() {
        removeAll();
        add(buildWelcome());
    }

    private Component buildWelcome() {
        AccountSettings account = settings.account();

        // ---- left: what Nei is, shown as a tiny preview of a day
        Div brand = new Div(new Span("N"));
        brand.addClassName("nei-brand");
        Div title = new Div(new Span("A calm place for your day."));
        title.addClassName("nei-art-title");
        Div chips = new Div(
                previewChip("08:00", "Plan the morning", true),
                previewChip("09:30", "Deep work", false),
                previewChip("14:00", "Write today's note", false));
        chips.addClassName("nei-art-chips");
        Div foot = new Div(new Span("Everything stays on this computer."));
        foot.addClassName("nei-art-foot");
        Div art = new Div(brand, title, chips, foot);
        art.addClassName("nei-welcome-art");

        // ---- right: the form
        Div eyebrow = new Div(new Span("WELCOME"));
        eyebrow.addClassName("nei-eyebrow");
        H1 hello = new H1("Hi, I'm Nei.");
        Paragraph copy = new Paragraph("I'll help you plan your day, track what you finish, and look back on it.");

        TextField name = new TextField("What should I call you?");
        name.setPlaceholder("Your name");
        name.setAutofocus(true);
        name.setMaxLength(40);
        name.setWidthFull();

        RadioButtonGroup<String> week = new RadioButtonGroup<>();
        week.setLabel("Your week starts on");
        week.setItems("Monday", "Sunday");
        week.setValue(account.weekStart);

        Runnable start = () -> {
            if (name.getValue().isBlank()) {
                name.setInvalid(true);
                name.setErrorMessage("Tell me what to call you");
                return;
            }
            account.userName = name.getValue().trim();
            account.weekStart = week.getValue();
            settings.save();
            go("today");
        };
        name.addKeyPressListener(Key.ENTER, e -> start.run());

        Button go = new Button("Get started", new Icon(VaadinIcon.ARROW_RIGHT), e -> start.run());
        go.setIconAfterText(true);
        go.addClassName("nei-btn-primary");
        go.setWidthFull();
        Span hint = new Span("You can change all of this later in Settings.");
        hint.addClassName("nei-form-hint");

        Div form = new Div(eyebrow, hello, copy, name, week, go, hint);
        form.addClassName("nei-welcome-form");

        Div window = new Div(art, form);
        window.addClassName("nei-win");
        Div shell = new Div(window);
        shell.addClassName("nei-welcome");
        return shell;
    }

    private Component previewChip(String time, String text, boolean done) {
        Span t = new Span(time);
        t.addClassName("t");
        Span nm = new Span(text);
        nm.addClassName("nm");
        Div chip = new Div(t, nm);
        nm.getElement().getStyle().set("flex", "1");
        if (done) {
            chip.add(new Icon(VaadinIcon.CHECK));
            chip.addClassName("done");
        }
        chip.addClassName("nei-prev-chip");
        return chip;
    }
}
