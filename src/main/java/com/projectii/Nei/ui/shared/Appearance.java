package com.projectii.Nei.ui.shared;

import com.projectii.Nei.ui.model.DeviceSettings;
import com.vaadin.flow.component.Component;

/**
 * Applies theme mode, sidebar transparency, accent colour and reduce-motion
 * to the whole page by toggling classes / a CSS variable on <html>.
 */
public final class Appearance {

    private Appearance() {
        // utility class — never instantiated
    }

    public static void apply(Component any, DeviceSettings dev) {
        if (any == null || dev == null) {
            return;
        }

        String mode   = (dev.themeMode == null) ? "light" : dev.themeMode.toLowerCase();
        String accent = (dev.accent   == null) ? "#f7c531" : dev.accent;

        any.getElement().executeJs(
                "const el = document.documentElement;"
                        + "el.classList.remove('theme-dark','theme-warm','sidebar-transparent','nei-reduce-motion');"
                        + "if ($1 !== 'light') el.classList.add('theme-' + $1);"
                        + "if ($2) el.classList.add('sidebar-transparent');"
                        + "if ($3) el.classList.add('nei-reduce-motion');"
                        + "el.style.setProperty('--nei-accent', $4);",
                mode, dev.sidebarTransparent, dev.reduceMotion, accent);
    }
}