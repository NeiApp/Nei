package com.projectii.Nei.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.UUID;

@Entity
@Table(name = "weekly_review")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class WeeklyReview {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne
    @JoinColumn(name = "weeks_id")
    private Weeks weeks;

    private int totalTasks;
    private int completedTasks;
    private int skippedTasks;
    private int streakDays;
    private String bestDay;


    private String summaryText;        // Spring AI fills later

    @CreationTimestamp
    @Column(updatable = false, name = "generated_at")
    private LocalDateTime generatedAt;


    public int getMonthValue() {
        return weeks.getStartDate().getMonthValue();
    }

    public int getYear() {
        return weeks.getStartDate().getYear();
    }
}