package com.projectii.Nei.ui.shared;

import com.projectii.Nei.ui.model.DeviceSettings;
import com.vaadin.flow.component.Component;

/** Applies accent colour and reduce-motion to the whole page. */
public final class Appearance {
    private Appearance() {}

    public static void apply(Component any, DeviceSettings dev) {
        any.getElement().executeJs("document.documentElement.style.setProperty('--nei-accent', $0);"
                + "document.documentElement.classList.toggle('nei-reduce-motion', $1);", dev.accent, dev.reduceMotion);
    }
}
