package com.projectii.Nei.ui.app;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;

/** "/" goes to Today (or to the welcome screen if there is no name yet). */
@Route("")
public class RootRedirect extends Div implements BeforeEnterObserver {
    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        event.forwardTo("today");
    }
}
