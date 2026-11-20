package com.projectii.Nei.service.serviceImpl;

import com.projectii.Nei.mapper.SettingMapper;
import com.projectii.Nei.model.Settings;
import com.projectii.Nei.payload.request.SettingRequest;
import com.projectii.Nei.payload.response.SettingResponse;
import com.projectii.Nei.repository.SettingRepository;
import com.projectii.Nei.service.SettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class SettingsServiceImpl implements SettingsService {
    private final SettingRepository settingRepository;
    private final SettingMapper settingMapper;

    @Override
    public void updateSettings(UUID id, SettingRequest settingRequest) {
        Settings setting = settingRepository.findById(id).orElseThrow(()-> new RuntimeException("invalid settings id"));
        settingMapper.updateFromRequest(settingRequest, setting);
    }

    @Override
    public SettingResponse addSettings(SettingRequest settingRequest) {
        if (settingRequest.userName().isEmpty()) {
            throw new RuntimeException("username is needed");
        }

        Settings setting = settingMapper.toEntity(settingRequest);
        settingRepository.save(setting);

        return settingMapper.toResponse(setting);
    }

}
