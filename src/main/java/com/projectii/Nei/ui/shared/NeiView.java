package com.projectii.Nei.ui.shared;

import com.vaadin.flow.component.html.Div;

/** Base for every page: rebuilds itself with refresh(), and can navigate with go(). */
public abstract class NeiView extends Div {
    protected NeiView() {
        setSizeFull();
    }

    protected abstract void refresh();

    protected void go(String route) {
        getUI().ifPresent(ui -> ui.navigate(route));
    }
}
