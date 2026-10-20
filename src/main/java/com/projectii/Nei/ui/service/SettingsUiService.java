package com.projectii.Nei.ui.service;

import com.projectii.Nei.ui.model.AccountSettings;
import com.projectii.Nei.ui.model.DeviceSettings;

/**
 * Settings and the person's name. Backend today: nothing.
 * Store AccountSettings and DeviceSettings separately so only the account part syncs later.
 */
public interface SettingsUiService {
    AccountSettings account();
    DeviceSettings device();

    /** Persist whatever was changed on account() and device(). */
    void save();

    /** Settings > Your data > Delete all data. */
    void deleteAllData();
}
