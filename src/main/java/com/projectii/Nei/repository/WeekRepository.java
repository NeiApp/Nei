package com.projectii.Nei.repository;

import com.projectii.Nei.enums.WeekStatus;
import com.projectii.Nei.model.Weeks;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface WeekRepository extends JpaRepository<Weeks, UUID> {
    Optional<Weeks> findByStatus(WeekStatus weekStatus);

    Optional<Weeks> findByStartDateLessThanEqualAndEndDateGreaterThanEqual(LocalDate date, LocalDate date1);
}
