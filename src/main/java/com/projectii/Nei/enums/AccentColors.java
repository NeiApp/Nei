package com.projectii.Nei.enums;

/** The accent swatches on the Settings screen. The hex value is what the UI puts into --nei-accent. */
public enum AccentColors {
    YELLOW("#f7c531"),
    ORANGE("#FF9B71"),
    PURPLE("#B28DFF"),
    CYAN("#00CCFF"),
    LIME("#E4EC8A");

    private final String hex;

    AccentColors(String hex) {
        this.hex = hex;
    }

    public String hex() {
        return hex;
    }

    public static AccentColors fromHex(String hex) {
        for (AccentColors c : values()) if (c.hex.equalsIgnoreCase(hex)) return c;
        return YELLOW;
    }
}
