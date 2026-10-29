package com.projectii.Nei.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "month_summary")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class MonthSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Field name: summaryMonth -> Getter: getSummaryMonth()
    @Column(name = "summary_month", nullable = false)
    private int summaryMonth;

    // Field name: summaryYear -> Getter: getSummaryYear()
    @Column(name = "summary_year", nullable = false)
    private int summaryYear;

    @Column(name = "total_tasks", nullable = false)
    private int totalTasks;

    @Column(name = "completed_tasks", nullable = false)
    private int completedTasks;

    @Column(name = "skipped_tasks", nullable = false)
    private int skippedTasks;

    @Column(name = "summary_text", length = 500)
    private String summaryText;

    @CreationTimestamp
    @Column(updatable = false, name = "generated_at")
    private LocalDateTime generatedAt;

    public float getCompletionRate() {
        return totalTasks > 0 ? (float) completedTasks / totalTasks * 100 : 0f;
    }
}