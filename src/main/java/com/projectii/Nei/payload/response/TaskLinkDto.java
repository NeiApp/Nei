package com.projectii.Nei.payload.response;

import java.util.UUID;

public record TaskLinkDto(UUID id, UUID fromTaskId) {}