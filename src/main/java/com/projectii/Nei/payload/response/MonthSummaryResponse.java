package com.projectii.Nei.payload.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record MonthSummaryResponse(UUID id,
                                   int month,
                                   int year,
                                   int totalTasks,
                                   int completedTasks,
                                   int skippedTasks,
                                   float completionRate,
                                   String summaryText,
                                   LocalDateTime generatedAt) {
}
