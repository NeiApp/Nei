package com.projectii.Nei.mapper;

import com.projectii.Nei.model.Task;
import com.projectii.Nei.model.TaskLink;
import com.projectii.Nei.payload.response.TaskLinkDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public abstract class SchedulerMapper {
    public abstract TaskLinkDto toResponse(TaskLink taskLink);
}
