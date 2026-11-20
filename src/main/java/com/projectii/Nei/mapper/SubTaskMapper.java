package com.projectii.Nei.mapper;

import com.projectii.Nei.model.SubTasks;
import com.projectii.Nei.payload.response.SubTaskResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public abstract class SubTaskMapper {
    public abstract SubTaskResponse toResponse(SubTasks subTasks);
}
