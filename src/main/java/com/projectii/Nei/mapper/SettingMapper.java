package com.projectii.Nei.mapper;

import com.projectii.Nei.model.Settings;
import com.projectii.Nei.payload.request.SettingRequest;
import com.projectii.Nei.payload.response.SettingResponse;
import org.h2.engine.Setting;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public abstract class SettingMapper {
    public abstract Settings toEntity(SettingRequest settingRequest);

    public abstract SettingResponse toResponse(Settings settings);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    public abstract void updateFromRequest(SettingRequest req, @MappingTarget Settings setting);
}
