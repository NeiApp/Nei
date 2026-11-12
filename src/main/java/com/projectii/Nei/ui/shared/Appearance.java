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

        String mode = (dev.themeMode == null) ? "light" : dev.themeMode.toLowerCase();

        // Guard: only a real hex colour may reach --nei-accent.
        String accent = (dev.accent == null
                || !dev.accent.matches("#([0-9a-fA-F]{3}|[0-9a-fA-F]{6}|[0-9a-fA-F]{8})"))
                ? "#f7c531" : dev.accent;

        // Vaadin executeJs parameters are ZERO-indexed: $0..$3 for 4 args.
        any.getElement().executeJs(
                "const el = document.documentElement;"
                        + "el.classList.remove('theme-dark','theme-warm','theme-true','theme-false',"
                        + "'sidebar-transparent','nei-reduce-motion');"
                        + "if ($0 !== 'light') el.classList.add('theme-' + $0);"
                        + "if ($1) el.classList.add('sidebar-transparent');"
                        + "if ($2) el.classList.add('nei-reduce-motion');"
                        + "el.style.setProperty('--nei-accent', $3);",
                mode, dev.sidebarTransparent, dev.reduceMotion, accent);
    }
}