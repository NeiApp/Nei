package com.projectii.Nei.controller;

import com.projectii.Nei.model.TaskLink;
import com.projectii.Nei.payload.request.CreateLinkRequest;
import com.projectii.Nei.payload.request.PositionRequest;
import com.projectii.Nei.payload.response.RunStateDto;
import com.projectii.Nei.payload.response.TaskLinkDto;
import com.projectii.Nei.service.SchedulerService;
import com.projectii.Nei.service.TaskLinkService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/schedule")
public class ScheduleController {
    private final SchedulerService schedulerService;
    private final TaskLinkService taskLinkService;

    // ---- threads ----
    @PostMapping("/links")
    public ResponseEntity<TaskLinkDto> createLink(@RequestBody CreateLinkRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(taskLinkService.createLink(req.fromId(), req.toId()));
    }

    @DeleteMapping("/links/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteLink(@PathVariable UUID id) {
        taskLinkService.deleteLink(id);
    }

    // ---- canvas positions ----
    @PatchMapping("/tasks/{taskId}/position")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updatePosition(@PathVariable UUID taskId, @RequestBody PositionRequest req) {
        schedulerService.updatePosition(taskId, req.x(), req.y());
    }

    // ---- runs: Play / Finished-next / Stop / Restart ----
    @PostMapping("/runs")                                   // Play
    public ResponseEntity<RunStateDto> play(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate day = (date != null) ? date : LocalDate.now();
        return ResponseEntity.status(HttpStatus.CREATED).body(schedulerService.play(day));
    }

    @GetMapping("/runs/{runId}")                            // feeds the "Now → next" banner
    public RunStateDto state(@PathVariable UUID runId) {
        return schedulerService.getState(runId);
    }

    @PostMapping("/runs/{runId}/finish")                    // "Finished, next →"
    public RunStateDto finish(@PathVariable UUID runId) {
        return schedulerService.finishCurrent(runId);
    }

    @PostMapping("/runs/{runId}/stop")                      // Stop
    public RunStateDto stop(@PathVariable UUID runId) {
        return schedulerService.stop(runId);
    }

    @PostMapping("/runs/{runId}/restart")                   // Restart
    public RunStateDto restart(@PathVariable UUID runId) {
        return schedulerService.restart(runId);
    }
}
