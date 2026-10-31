package com.projectii.Nei.mapper;

import com.projectii.Nei.model.Alarm;
import com.projectii.Nei.model.Task;
import com.projectii.Nei.payload.request.AlarmRequest;
import com.projectii.Nei.payload.request.CreateATask;
import com.projectii.Nei.payload.request.UpdateTask;
import com.projectii.Nei.payload.response.CreatedTask;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public abstract class TaskMapper {
   public abstract Task toEntity(CreateATask createATask);

   public abstract CreatedTask toTaskLinkResponse(Task task);

   @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
   public abstract void update(UpdateTask req, @MappingTarget Task task);

//   @Mapping(target = "alarm", ignore = true)
   public abstract Alarm toAlarm(AlarmRequest req);
}
