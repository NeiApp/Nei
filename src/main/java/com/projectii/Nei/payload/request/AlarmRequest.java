package com.projectii.Nei.payload.request;

import java.time.LocalDateTime;

public record AlarmRequest(int intervalMinutes, LocalDateTime startAt,boolean active,boolean recurring) {
}
