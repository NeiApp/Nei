package com.projectii.Nei.mapper;

import com.projectii.Nei.model.Alarm;
import com.projectii.Nei.model.Task;
import com.projectii.Nei.payload.request.AlarmRequest;
import com.projectii.Nei.payload.request.CreateATask;
import com.projectii.Nei.payload.response.CreatedTask;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public abstract class TaskMapper {
   public abstract Task toEntity(CreateATask createATask);

   public abstract CreatedTask toResponse(Task task);

   public abstract Alarm toAlarm(AlarmRequest alarmRequest);
}
