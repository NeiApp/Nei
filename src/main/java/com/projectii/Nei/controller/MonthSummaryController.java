package com.projectii.Nei.controller;

import com.projectii.Nei.payload.response.MonthSummaryResponse;
import com.projectii.Nei.service.serviceImpl.MonthSummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/monthlysummary")
public class MonthSummaryController {

    private final MonthSummaryService monthSummaryService;

    @GetMapping("/{month}/{year}")
    public ResponseEntity<MonthSummaryResponse> getMonthSummary(
            @PathVariable int month,
            @PathVariable int year
    ) {
        return ResponseEntity.ok(
                monthSummaryService.getMonthSummary(month, year)
        );
    }
}
