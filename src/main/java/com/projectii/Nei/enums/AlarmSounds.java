package com.projectii.Nei.enums;

/** The alarm sounds on the Settings screen. The label is the text the UI shows. */
public enum AlarmSounds {
    SOFT_CHIME("Soft chime"),
    BELL("Bell"),
    PULSE("Pulse"),
    SILENT("Silent");

    private final String label;

    AlarmSounds(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static AlarmSounds fromLabel(String label) {
        for (AlarmSounds s : values()) if (s.label.equalsIgnoreCase(label)) return s;
        return SOFT_CHIME;
    }
}
