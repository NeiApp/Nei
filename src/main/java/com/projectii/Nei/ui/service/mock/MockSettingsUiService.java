package com.projectii.Nei.ui.service.mock;

import com.projectii.Nei.ui.model.*;
import com.projectii.Nei.ui.service.*;
import org.springframework.stereotype.Service;

/** MOCK: settings live in memory and reset on restart. Persist them (account and device separately). */
@Service
public class MockSettingsUiService implements SettingsUiService {
    private final AccountSettings account = new AccountSettings();
    private final DeviceSettings device = new DeviceSettings();
    private final TaskUiService tasks;
    private final NoteUiService notes;

    public MockSettingsUiService(TaskUiService tasks, NoteUiService notes) {
        this.tasks = tasks;
        this.notes = notes;
    }

    @Override public AccountSettings account() { return account; }
    @Override public DeviceSettings device() { return device; }
    @Override public void save() { /* mock: nothing to persist */ }

    @Override public void deleteAllData() {
        tasks.all().clear(); // mock only: the real version deletes rows
        notes.all().clear();
    }
}
