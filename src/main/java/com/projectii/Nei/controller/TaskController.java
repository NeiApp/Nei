package com.projectii.Nei.controller;

import com.projectii.Nei.payload.request.AddNoteRequest;
import com.projectii.Nei.payload.request.AlarmRequest;
import com.projectii.Nei.payload.request.CreateATask;
import com.projectii.Nei.payload.request.SubTaskRequest;
import com.projectii.Nei.payload.response.AlarmResponse;
import com.projectii.Nei.payload.response.CreatedTask;
import com.projectii.Nei.payload.response.SubTaskResponse;
import com.projectii.Nei.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * this
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/task")
public class TaskController {

    private final TaskService taskService;

    @PostMapping("/create")
    public ResponseEntity<CreatedTask> createATaskForAWeekDay(
            @RequestBody CreateATask createATask
    ) {
        return new ResponseEntity<>(
                taskService.createTask(createATask),
                HttpStatus.CREATED
        );
    }


    @PatchMapping("/add-subtasks/{id}")
    public ResponseEntity<CreatedTask> addSubtasks (@PathVariable UUID taskId,@RequestBody List<SubTaskRequest> subTaskRequest){
        return ResponseEntity.ok(taskService.addSubTasks(taskId,subTaskRequest));
    }


    @GetMapping("/today")
    public ResponseEntity<List<CreatedTask>> getTodayTasks() {
        return ResponseEntity.ok(taskService.getTodayTasks());
    }

    @PatchMapping("/{id}/complete")
    public ResponseEntity<Void> completeTask(@PathVariable UUID id) {
        taskService.completeATaskHandler(id);
        return ResponseEntity.ok().build();
    }

    /**
     *
     * @param id
     * @param request
     * @return
     */
    @PatchMapping("/{id}/note")
    public ResponseEntity<Void> addNote(
            @PathVariable UUID id,
            @RequestBody AddNoteRequest request
    ) {
        taskService.addNoteToTask(id, request.note());
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/alarm")
    public ResponseEntity<AlarmResponse> alarmScheduler(@PathVariable UUID id, @RequestBody AlarmRequest alarmRequest){
        AlarmResponse alarmResponse = taskService.createAlarmReminder(id,alarmRequest);
        return new ResponseEntity<>(alarmResponse, HttpStatus.CREATED);
    }


    @DeleteMapping("/{id}/delete")
    public ResponseEntity<?> deleteATask(@PathVariable UUID id){
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }



    
}
