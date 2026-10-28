package com.projectii.Nei.model;


import com.projectii.Nei.enums.TaskCategory;
import com.projectii.Nei.enums.TaskStatus;
import com.vaadin.copilot.shaded.checkerframework.checker.units.qual.C;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "Task", indexes = @Index(name = "idx_task_date", columnList = "date"))
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Task {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String title;

    private String description;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private TaskStatus status;

    @Column(name = "recurring")
    private boolean recurring;

    @Column(name = "date")
    private LocalDate date;

    @CreationTimestamp
    @Column(updatable = false, name = "created_at")
    private Instant createdAt;

    private String note;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "alarm_id")
    private Alarm alarm;

    @ManyToOne
    @JoinColumn(name = "week_day_id")
    private WeekDay weekDay;

    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Tags> tag = new ArrayList<>();

    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SubTasks> subTasks = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private TaskCategory category = TaskCategory.PERSONAL;

    public void addSubTask(SubTasks subTask) {
        subTasks.add(subTask);
        subTask.setTask(this);
    }

    public void addSubTasks(List<SubTasks> newSubTasks) {
        newSubTasks.forEach(this::addSubTask);
    }
}
