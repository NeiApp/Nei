package com.projectii.Nei.ui.app;

import com.projectii.Nei.service.serviceImpl.AlarmBroadcaster;
import com.projectii.Nei.ui.service.SettingsUiService;
import com.projectii.Nei.ui.shared.Appearance;
import com.projectii.Nei.ui.welcome.WelcomeView;   // << ADDED: type-safe forward target
import com.vaadin.flow.component.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.shared.Registration;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * The window frame: icon rail on the left, the current page on the right, a status bar at the bottom.
 * Alt+1 ... Alt+6 jump between pages.
 */
@CssImport("./styles/base.css")
@CssImport("./styles/rail.css")
public class MainLayout extends Div implements RouterLayout, BeforeEnterObserver, AfterNavigationObserver {

    private final SettingsUiService settings;
    private final Div content = new Div();
    private final Map<String, Button> railButtons = new HashMap<>();
    private final AlarmBroadcaster broadcaster;
    private Registration alarmReg;

    public MainLayout(SettingsUiService settings, AlarmBroadcaster broadcaster) {
        this.settings = settings;
        this.broadcaster = broadcaster;
        addClassName("nei-app");
        setSizeFull();

        Div rail = new Div();
        rail.addClassName("nei-rail");

        // brand tile — recolours with the accent, ties the rail to the welcome mark
        Div brand = new Div(new Span("N"));
        brand.addClassName("nei-rail-brand");
        brand.getElement().setAttribute("title", "Nei");

        Div top = new Div(
                railBtn(VaadinIcon.COFFEE, "Focus", null),
                railBtn(VaadinIcon.LIST_UL, "Today (Alt+1)", "today"),
                railBtn(VaadinIcon.CALENDAR_O, "Calendar (Alt+2)", "calendar"),
                railBtn(VaadinIcon.NOTEBOOK, "Notes (Alt+3)", "notes"),
                railBtn(VaadinIcon.CLUSTER, "Schedule (Alt+4)", "schedule"),
                railBtn(VaadinIcon.CHART_LINE, "Review (Alt+5)", "review"),
                railBtn(VaadinIcon.MAGIC, "Ask Nei", null),
                railBtn(VaadinIcon.ELLIPSIS_DOTS_H, "More", null));
        Div bottom = new Div(
                railBtn(VaadinIcon.CHECK, "Completed", null),
                railBtn(VaadinIcon.COG_O, "Settings (Alt+6)", "settings"));
        top.addClassName("nei-rail-group");
        bottom.addClassName("nei-rail-group");
        rail.add(brand, top, bottom);

        content.addClassName("nei-content");
        Div body = new Div(rail, content);
        body.addClassName("nei-body");
        add(body, buildStatusBar());

        shortcut(Key.DIGIT_1, "today");
        shortcut(Key.DIGIT_2, "calendar");
        shortcut(Key.DIGIT_3, "notes");
        shortcut(Key.DIGIT_4, "schedule");
        shortcut(Key.DIGIT_5, "review");
        shortcut(Key.DIGIT_6, "settings");
        Appearance.apply(this, settings.device());
    }

    private Div buildStatusBar() {
        Span dot = new Span();
        dot.addClassName("nei-status-dot");
        Span mode = new Span(dot, new Span("Local mode"));
        mode.addClassName("nei-status-item");
        // TODO(sync): show real sync state here once the phone companion exists
        Span sync = new Span("Sync: off");
        Span spacer = new Span();
        spacer.addClassName("nei-status-grow");
        Span date = new Span(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d")));
        Span version = new Span("Nei v0.1");
        Div bar = new Div(mode, sync, spacer, date, version);
        bar.addClassName("nei-statusbar");
        return bar;
    }

    private void shortcut(Key key, String route) {
        Shortcuts.addShortcutListener(this, () -> getUI().ifPresent(ui -> ui.navigate(route)), key, KeyModifier.ALT);
    }

    private Button railBtn(VaadinIcon icon, String tip, String route) {
        Button b = new Button(new Icon(icon));
        b.addClassName("nei-rail-btn");
        b.getElement().setAttribute("title", tip);
        b.getElement().setAttribute("aria-label", tip);
        b.addClickListener(e -> {
            if (route == null) Notification.show(tip + " is coming soon");
            else getUI().ifPresent(ui -> ui.navigate(route));
        });
        if (route != null) railButtons.put(route, b);
        return b;
    }

    /** Put the current page into the content area. */
    @Override
    public void showRouterLayoutContent(HasElement page) {
        content.getElement().removeAllChildren();
        if (page != null) content.getElement().appendChild(page.getElement());
    }

    /** Nobody gets past the welcome screen without a name. */
    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (settings.account().userName == null) event.forwardTo(WelcomeView.class);   // << CHANGED: was forwardTo("welcome")
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        String current = event.getLocation().getFirstSegment();
        railButtons.forEach((route, b) -> {
            if (route.equals(current)) b.addClassName("active"); else b.removeClassName("active");
        });
    }

    @Override
    protected void onAttach(AttachEvent e) {
        UI ui = e.getUI();
        alarmReg = broadcaster.register(n ->
                ui.access(() -> Notification.show(n.message(), 8000, Notification.Position.TOP_END)));
    }

    @Override
    protected void onDetach(DetachEvent e) { if (alarmReg != null) alarmReg.remove(); }
}