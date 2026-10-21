package com.projectii.Nei.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "alarm")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Alarm {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private int intervalMinutes;

    private LocalDateTime startAt;

    @Column(nullable = true)
    private LocalDateTime stopAt;

    private LocalDateTime nextFireAt;

    private boolean active;
}
