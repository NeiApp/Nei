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

    private int month;
    private int year;

    // snapshot values
    private int totalTasks;
    private int completedTasks;
    private int skippedTasks;      

    private String summaryText;    // Spring AI fills later

    @CreationTimestamp
    @Column(updatable = false, name = "generated_at")
    private LocalDateTime generatedAt;

    public float getCompletionRate() {
        return totalTasks > 0 ? (float) completedTasks / totalTasks * 100 : 0f;
    }

}