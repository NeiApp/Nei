package com.projectii.Nei.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "subtasks")
@Getter
@Setter
@NoArgsConstructor
public class SubTasks {
    @Id
    @GeneratedValue (strategy = GenerationType.UUID)
    private UUID id;
    private String nameOfSubTask;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id")
    private Task task;
}
