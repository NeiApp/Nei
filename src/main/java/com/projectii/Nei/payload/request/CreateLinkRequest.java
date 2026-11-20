package com.projectii.Nei.payload.request;

import java.util.UUID;

public record CreateLinkRequest(UUID fromId, UUID toId) {}