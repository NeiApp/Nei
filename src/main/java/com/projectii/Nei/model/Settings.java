package com.projectii.Nei.model;

import com.projectii.Nei.enums.AccentColors;
import com.projectii.Nei.enums.AlarmSounds;
import com.projectii.Nei.enums.ClockFormat;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.UUID;

@Entity
@Table(name = "settings")
@Getter
@Setter
@NoArgsConstructor
public class Settings {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // ---- Profile and time (account: should sync to the phone)
    private String userName;

    @Column(nullable = false)
    private String timeZoneId = ZoneId.systemDefault().getId();   // use ZoneId.of(timeZoneId) when you need it

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DayOfWeek startOfTheWeek = DayOfWeek.MONDAY;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClockFormat clockFormat = ClockFormat.H24;

    // ---- Your day (account)
    @Column(nullable = false)
    private LocalTime dayStarts = LocalTime.of(8, 0);

    @Column(nullable = false)
    private LocalTime dayEnds = LocalTime.of(18, 0);

    @Column(nullable = false)
    private int overloadedWarning = 5;            // open tasks per day before Nei warns you

    // ---- Reminders and alarms (account)
    private boolean notification = true;
    private int defaultAlarmInterval = 60;        // minutes
    private int snoozeLength = 10;                // minutes
    private LocalTime quietHoursFrom = LocalTime.of(22, 0);
    private LocalTime quietHoursUntil = LocalTime.of(7, 0);
    private boolean askWhenTaskEnds = true;

    // ---- Reflection and reviews (account)
    private LocalTime reflectionTime = LocalTime.of(21, 0);

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DayOfWeek weeklyReviewDay = DayOfWeek.SUNDAY;

    // ---- This device only (should NOT sync)
    @Enumerated(EnumType.STRING)
    private AccentColors accentColor = AccentColors.YELLOW;

    private boolean reduceMotion;
    private boolean launchAtLogin;
    private boolean keepRunningInTheTray = true;
    private String quickAddShortcut = "Ctrl+Alt+N";

    @Enumerated(EnumType.STRING)
    private AlarmSounds alarmSound = AlarmSounds.SOFT_CHIME;
}
