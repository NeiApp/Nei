package com.projectii.Nei.repository;

import com.projectii.Nei.model.WeeklyReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WeeklyReviewRepository extends JpaRepository<WeeklyReview, UUID> {
        @Query("""
        SELECT wr FROM WeeklyReview wr
        WHERE FUNCTION('MONTH', wr.weeks.startDate) = :month
        AND FUNCTION('YEAR', wr.weeks.startDate) = :year
    """)
        List<WeeklyReview> findAllByMonthAndYear(int month, int year);
}
