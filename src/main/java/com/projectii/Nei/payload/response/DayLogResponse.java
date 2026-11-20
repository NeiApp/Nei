package com.projectii.Nei.payload.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record DayLogResponse(UUID id, String reflection, LocalDateTime loggedAt) {

}
