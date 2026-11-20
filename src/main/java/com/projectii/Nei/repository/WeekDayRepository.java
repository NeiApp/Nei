package com.projectii.Nei.repository;

import com.projectii.Nei.model.WeekDay;
import com.projectii.Nei.model.Weeks;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.DayOfWeek;
import java.util.Optional;
import java.util.UUID;

public interface WeekDayRepository extends JpaRepository<WeekDay, UUID> {
    Optional<WeekDay> findByWeeksAndDayOfWeek(Weeks createATask, DayOfWeek weekDay);
}
