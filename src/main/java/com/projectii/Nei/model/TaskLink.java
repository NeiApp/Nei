package com.projectii.Nei.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "task_link", uniqueConstraints = @UniqueConstraint(columnNames = {"from_task_id", "to_task_id"}))
@Getter
@Setter @AllArgsConstructor
@NoArgsConstructor
public class TaskLink {
    @Id
    @GeneratedValue (strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "from_task_id")
    private Task fromTask;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "to_task_id")
    private Task toTask;
}
