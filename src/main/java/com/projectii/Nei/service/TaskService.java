package com.projectii.Nei.service;

import com.projectii.Nei.payload.request.AlarmRequest;
import com.projectii.Nei.payload.request.CreateATask;
import com.projectii.Nei.payload.request.SubTaskRequest;
import com.projectii.Nei.payload.response.AlarmResponse;
import com.projectii.Nei.payload.response.CreatedTask;
import com.projectii.Nei.payload.response.SubTaskResponse;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public interface TaskService {

    CreatedTask createTask(CreateATask createATask);
    
    void completeATaskHandler(UUID id);

    List<CreatedTask> getTodayTasks();

    void addNoteToTask(UUID id, String note);

    AlarmResponse createAlarmReminder(UUID id, AlarmRequest alarmRequest);

    void deleteTask(UUID id);

    CreatedTask addSubTasks(UUID taskId, List<SubTaskRequest> subTaskRequest);
}
