package com.projectii.Nei.repository;

import com.projectii.Nei.model.MonthSummary;
import com.projectii.Nei.service.serviceImpl.MonthSummaryService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MonthSummaryRepository extends JpaRepository<MonthSummary, UUID> {
    Boolean existsByMonthAndYear(int month, int year);

    Optional<MonthSummary> findByMonthAndYear(int month, int year);
}
