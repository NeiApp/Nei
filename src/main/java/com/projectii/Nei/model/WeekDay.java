package com.projectii.Nei.model;

import com.projectii.Nei.enums.TaskStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "week_day")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class WeekDay {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "week_id")
    private Weeks weeks;

    private LocalDate date;

    @Enumerated(EnumType.STRING)
    private DayOfWeek dayOfWeek;

    @OneToMany(mappedBy = "weekDay", cascade = CascadeType.ALL)
    private List<Task> tasks;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "day_log_id")
    private DayLog dayLog;


    
    public int getTotalTasks() {
        return tasks != null ? tasks.size() : 0;
    }

    public int getCompletedTasks() {
        return tasks != null
                ? (int) tasks.stream()
                .filter(t -> t.getStatus() == TaskStatus.DONE)
                .count()
                : 0;
    }
}