package com.projectii.Nei.controller;

import com.projectii.Nei.payload.request.DayLogReflectionRequest;
import com.projectii.Nei.payload.response.DayLogResponse;
import com.projectii.Nei.service.serviceImpl.DayServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/daylog")
public class DayLogController {

    private final DayServiceImpl dayService;

    @PatchMapping("/{id}/reflection")
    public ResponseEntity<?> reflectOnYourDay(@PathVariable UUID id, @RequestBody DayLogReflectionRequest request){
        dayService.submitReflection(id, request.reflection());
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<DayLogResponse> getTodayReflection() {
        return ResponseEntity.ok(dayService.getTodayDayLog());
    }
}
