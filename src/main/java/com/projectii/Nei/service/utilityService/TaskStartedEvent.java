package com.projectii.Nei.service.utilityService;

import java.util.UUID;

public record TaskStartedEvent(UUID runId, UUID taskId, String title) {}
