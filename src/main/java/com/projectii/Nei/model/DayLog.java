package com.projectii.Nei.model;

import com.projectii.Nei.enums.TaskStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "day_log")  // renamed from task_log — cleaner
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class DayLog {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(mappedBy = "dayLog")
    private WeekDay weekDay;

    private String reflection;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime loggedAt;

    public int getTotalTasks() {
        return weekDay != null ? weekDay.getTotalTasks() : 0;
    }

    public int getCompletedTasks() {
        return weekDay != null ? weekDay.getCompletedTasks() : 0;
    }

    public int getSkippedTasks() {
        return weekDay != null
                ? (int) weekDay.getTasks().stream()
                .filter(t -> t.getStatus() == TaskStatus.SKIPPED)
                .count()
                : 0;
    }
}