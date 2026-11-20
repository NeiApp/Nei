package com.projectii.Nei.service;

import com.projectii.Nei.payload.request.SettingRequest;
import com.projectii.Nei.payload.response.SettingResponse;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public interface SettingsService {
    void updateSettings(UUID id, SettingRequest settingRequest);

    SettingResponse addSettings(SettingRequest settingRequest);
}
