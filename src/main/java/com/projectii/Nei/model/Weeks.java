package com.projectii.Nei.model;

import com.projectii.Nei.enums.WeekStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "week")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Weeks {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private LocalDate startDate;
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    private WeekStatus status;

    @OneToMany(mappedBy = "weeks", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WeekDay> days;

    @OneToOne(mappedBy = "weeks")
    private WeeklyReview weeklyReview;


}
