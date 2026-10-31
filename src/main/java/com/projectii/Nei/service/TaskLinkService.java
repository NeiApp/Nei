package com.projectii.Nei.service;

import com.projectii.Nei.model.TaskLink;
import com.projectii.Nei.payload.response.TaskLinkDto;

import java.util.UUID;

public interface TaskLinkService {
    TaskLinkDto createLink(UUID fromId, UUID toId);

    void deleteLink(UUID linkId);
}
