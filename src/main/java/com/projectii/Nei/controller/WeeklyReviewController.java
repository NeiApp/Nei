package com.projectii.Nei.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import com.projectii.Nei.payload.response.WeeklyReviewResponse;
import com.projectii.Nei.service.serviceImpl.WeekService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/weekly")
@RequiredArgsConstructor
public class WeeklyReviewController {

    private final WeekService weekService;

    @GetMapping("/{id}")
    public ResponseEntity<WeeklyReviewResponse> getAllTheWeeklyReview (@PathVariable("id") UUID weekId){
        return ResponseEntity.ok(weekService.getWeeklyReview(weekId));
    }
}
