package com.projectii.Nei.controller;

import com.projectii.Nei.payload.request.*;
import com.projectii.Nei.payload.response.AlarmResponse;
import com.projectii.Nei.payload.response.CreatedTask;
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


    @PatchMapping("/add-subtasks/{taskId}")
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

    @PatchMapping("/{id}")
    public ResponseEntity<CreatedTask> updateTask(@PathVariable  UUID id,  @RequestBody UpdateTask req){
        return ResponseEntity.ok(taskService.updateTask(id,req));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Void> status(@PathVariable UUID id, @RequestBody StatusRequest statusRequest){
        taskService.updateStatus(id,statusRequest);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}/delete")
    public ResponseEntity<?> deleteATask(@PathVariable UUID id){
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }
}
