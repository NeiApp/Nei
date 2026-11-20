package com.projectii.Nei.model;

import com.projectii.Nei.enums.RunStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "schedule_run")
@Getter @Setter @AllArgsConstructor
@NoArgsConstructor
public class ScheduleRun {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private LocalDate date;

    @Enumerated(EnumType.STRING)
    private RunStatus status;            // RUNNING, STOPPED, COMPLETED

    @ManyToOne(fetch = FetchType.LAZY)
    private Task currentTask;

    private Instant lastReminderAt;
}
