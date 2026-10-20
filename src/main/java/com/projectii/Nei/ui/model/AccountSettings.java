package com.projectii.Nei.ui.model;

import java.time.LocalTime;

/** Settings that follow the person to the phone (sync these). */
public class AccountSettings {
    public String userName; // null until the welcome screen is completed
    public String timezone = java.time.ZoneId.systemDefault().getId();
    public String weekStart = "Monday";
    public boolean use24h = true;
    public LocalTime workStart = LocalTime.of(8, 0), workEnd = LocalTime.of(18, 0);
    public int maxTasksPerDay = 5;
    public boolean notifications = true, askWhenDone = true;
    public int defaultAlarmMin = 60, snoozeMin = 10;
    public LocalTime quietFrom = LocalTime.of(22, 0), quietTo = LocalTime.of(7, 0);
    public LocalTime reflectionTime = LocalTime.of(21, 0);
    public String reviewDay = "Sunday";
}
