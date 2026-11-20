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
   // CreateATask calls it "taskCategory", the entity calls it "category": without this the category was dropped
   @Mapping(source = "taskCategory", target = "category")
   public abstract Task toEntity(CreateATask createATask);

   // same name mismatch on the way out, plus subTasks -> subTask (the response was always returning null for both)
   @Mapping(source = "category", target = "taskCategory")
   @Mapping(source = "subTasks", target = "subTask")
   public abstract CreatedTask toTaskLinkResponse(Task task);

   @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
   public abstract void update(UpdateTask req, @MappingTarget Task task);

//   @Mapping(target = "alarm", ignore = true)
   public abstract Alarm toAlarm(AlarmRequest req);
}
