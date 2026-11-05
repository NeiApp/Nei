package com.projectii.Nei.controller;

import com.projectii.Nei.payload.request.SettingRequest;
import com.projectii.Nei.payload.response.SettingResponse;
import com.projectii.Nei.service.SettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/settings")
public class SettingController {
    private final SettingsService settingsService;

    public ResponseEntity<SettingResponse> setting(@RequestBody SettingRequest settingRequest){
        return new ResponseEntity<>(settingsService.addSettings(settingRequest),HttpStatus.CREATED);
    }

    @PatchMapping("/{id}/update-settings")
    public ResponseEntity<SettingResponse> updateSettings(@PathVariable UUID id, SettingRequest settingRequest){
        settingsService.updateSettings(id,settingRequest);
        return ResponseEntity.ok().build();
    }

}
