package com.projectii.Nei.payload.response;

import com.projectii.Nei.enums.AccentColors;
import com.projectii.Nei.enums.AlarmSounds;
import com.projectii.Nei.enums.ClockFormat;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

public record SettingResponse(
        UUID id,
        String userName,
        String timeZoneId,
        DayOfWeek startOfTheWeek,
        ClockFormat clockFormat,
        LocalTime dayStarts,
        LocalTime dayEnds,
        int overloadedWarning,
        boolean notification,
        int defaultAlarmInterval,        // minutes
        int snoozeLength,            // minutes
        LocalTime quietHoursFrom,
        LocalTime quietHoursUntil,
        boolean askWhenTaskEnds,
        LocalTime reflectionTime,
        DayOfWeek weeklyReviewDay,
        AccentColors accentColor,
        boolean reduceMotion,
        boolean launchAtLogin,
        boolean keepRunningInTheTray,
        String quickAddShortcut,
        AlarmSounds alarmSound
) {
}
