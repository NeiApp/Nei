package com.projectii.Nei.payload.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record WeeklyReviewResponse(UUID id,
                                   int totalTasks,
                                   int completedTasks,
                                   int skippedTasks,
                                   int streakDays,
                                   String bestDay,
                                   String summaryText,
                                   LocalDateTime generatedAt) {
}