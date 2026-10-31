package com.projectii.Nei.service.utilityService;

import java.util.UUID;

public record TaskReminderEvent(UUID runId, UUID taskId, String title) {}