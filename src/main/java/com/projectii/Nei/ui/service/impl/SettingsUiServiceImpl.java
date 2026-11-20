package com.projectii.Nei.ui.service.impl;

import com.projectii.Nei.enums.AccentColors;
import com.projectii.Nei.enums.AlarmSounds;
import com.projectii.Nei.enums.ClockFormat;
import com.projectii.Nei.model.Settings;
import com.projectii.Nei.repository.ScheduleRunRepository;
import com.projectii.Nei.repository.SettingRepository;
import com.projectii.Nei.repository.TaskLinkRepository;
import com.projectii.Nei.repository.TaskRepository;
import com.projectii.Nei.ui.model.AccountSettings;
import com.projectii.Nei.ui.model.DeviceSettings;
import com.projectii.Nei.ui.service.NoteUiService;
import com.projectii.Nei.ui.service.SettingsUiService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.DayOfWeek;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Locale;
import java.util.UUID;

/**
 * The real SettingsUiService. There is one settings row (single-user desktop app).
 * The screens edit the AccountSettings / DeviceSettings objects directly and call save(),
 * so this class keeps one instance of each and copies them to and from that row.
 */
@Service
@Profile("!mock")
public class SettingsUiServiceImpl implements SettingsUiService {

    private final SettingRepository settingRepo;
    private final TaskRepository taskRepo;
    private final TaskLinkRepository linkRepo;
    private final ScheduleRunRepository runRepo;
    private final NoteUiService notes;
    private final ApplicationEventPublisher events;
    private final TransactionTemplate tx;

    private final AccountSettings account = new AccountSettings();
    private final DeviceSettings device = new DeviceSettings();
    private boolean loaded;
    private UUID rowId;

    public SettingsUiServiceImpl(SettingRepository settingRepo, TaskRepository taskRepo, TaskLinkRepository linkRepo,
                                 ScheduleRunRepository runRepo, NoteUiService notes,
                                 ApplicationEventPublisher events, PlatformTransactionManager txManager) {
        this.settingRepo = settingRepo;
        this.taskRepo = taskRepo;
        this.linkRepo = linkRepo;
        this.runRepo = runRepo;
        this.notes = notes;
        this.events = events;
        this.tx = new TransactionTemplate(txManager);
    }

    @Override
    public synchronized AccountSettings account() {
        ensureLoaded();
        return account;
    }

    @Override
    public synchronized DeviceSettings device() {
        ensureLoaded();
        return device;
    }

    @Override
    public synchronized void save() {
        ensureLoaded();
        rowId = tx.execute(status -> {
            Settings s = rowId == null ? new Settings() : settingRepo.findById(rowId).orElseGet(Settings::new);
            toEntity(s);
            return settingRepo.save(s).getId();
        });
    }

    @Override
    public synchronized void deleteAllData() {
        tx.executeWithoutResult(status -> {
            runRepo.deleteAll();     // runs and threads point at tasks (foreign keys), so they go first
            linkRepo.deleteAll();
            taskRepo.deleteAll();    // takes each task's note, alarm and subtasks with it
        });
        new ArrayList<>(notes.all()).forEach(notes::delete);
        events.publishEvent(new TasksChangedEvent());   // the task screen must not show a stale snapshot
    }

    // ------------------------------------------------------------------ row <-> screen objects

    private void ensureLoaded() {
        if (loaded) return;
        settingRepo.findAll().stream().findFirst().ifPresent(this::fromEntity);
        loaded = true;
    }

    private void fromEntity(Settings s) {
        rowId = s.getId();

        account.userName = s.getUserName();   // stays null until the welcome screen is completed
        account.timezone = nvl(s.getTimeZoneId(), account.timezone);
        account.weekStart = dayName(s.getStartOfTheWeek(), account.weekStart);
        account.use24h = s.getClockFormat() != ClockFormat.H12;
        account.workStart = nvl(s.getDayStarts(), account.workStart);
        account.workEnd = nvl(s.getDayEnds(), account.workEnd);
        account.maxTasksPerDay = s.getOverloadedWarning();
        account.notifications = s.isNotification();
        account.askWhenDone = s.isAskWhenTaskEnds();
        account.defaultAlarmMin = s.getDefaultAlarmInterval();
        account.snoozeMin = s.getSnoozeLength();
        account.quietFrom = nvl(s.getQuietHoursFrom(), account.quietFrom);
        account.quietTo = nvl(s.getQuietHoursUntil(), account.quietTo);
        account.reflectionTime = nvl(s.getReflectionTime(), account.reflectionTime);
        account.reviewDay = dayName(s.getWeeklyReviewDay(), account.reviewDay);

        device.launchAtLogin = s.isLaunchAtLogin();
        device.minimizeToTray = s.isKeepRunningInTheTray();
        device.reduceMotion = s.isReduceMotion();
        device.quickAddShortcut = nvl(s.getQuickAddShortcut(), device.quickAddShortcut);
        device.sound = nvl(s.getAlarmSound(), AlarmSounds.SOFT_CHIME).label();
        device.accent = nvl(s.getAccentColor(), AccentColors.YELLOW).hex();
        device.themeMode = nvl(s.getThemeMode(), "light");
        device.sidebarTransparent = s.isSidebarTransparent();
    }

    private void toEntity(Settings s) {
        s.setUserName(account.userName);
        s.setTimeZoneId(nvl(account.timezone, ZoneId.systemDefault().getId()));
        s.setStartOfTheWeek(day(account.weekStart, DayOfWeek.MONDAY));
        s.setClockFormat(account.use24h ? ClockFormat.H24 : ClockFormat.H12);
        s.setDayStarts(account.workStart);
        s.setDayEnds(account.workEnd);
        s.setOverloadedWarning(account.maxTasksPerDay);
        s.setNotification(account.notifications);
        s.setAskWhenTaskEnds(account.askWhenDone);
        s.setDefaultAlarmInterval(account.defaultAlarmMin);
        s.setSnoozeLength(account.snoozeMin);
        s.setQuietHoursFrom(account.quietFrom);
        s.setQuietHoursUntil(account.quietTo);
        s.setReflectionTime(account.reflectionTime);
        s.setWeeklyReviewDay(day(account.reviewDay, DayOfWeek.SUNDAY));

        s.setLaunchAtLogin(device.launchAtLogin);
        s.setKeepRunningInTheTray(device.minimizeToTray);
        s.setReduceMotion(device.reduceMotion);
        s.setQuickAddShortcut(device.quickAddShortcut);
        s.setAlarmSound(AlarmSounds.fromLabel(device.sound));
        s.setAccentColor(AccentColors.fromHex(device.accent));
        s.setThemeMode(device.themeMode);
        s.setSidebarTransparent(device.sidebarTransparent);
    }

    private static <T> T nvl(T value, T fallback) {
        return value != null ? value : fallback;
    }

    private static String dayName(DayOfWeek d, String fallback) {
        return d == null ? fallback : d.getDisplayName(TextStyle.FULL, Locale.ENGLISH);
    }

    private static DayOfWeek day(String name, DayOfWeek fallback) {
        try {
            return DayOfWeek.valueOf(name.trim().toUpperCase(Locale.ROOT));
        } catch (RuntimeException e) {   // null or not a day name
            return fallback;
        }
    }
}
