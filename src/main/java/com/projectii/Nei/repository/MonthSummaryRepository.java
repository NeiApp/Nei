package com.projectii.Nei.repository;

import com.projectii.Nei.model.MonthSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MonthSummaryRepository extends JpaRepository<MonthSummary, UUID> {

    // MUST match Entity field names: summaryMonth AND summaryYear
    Boolean existsBySummaryMonthAndSummaryYear(int summaryMonth, int summaryYear);

    Optional<MonthSummary> findBySummaryMonthAndSummaryYear(int summaryMonth, int summaryYear);
}