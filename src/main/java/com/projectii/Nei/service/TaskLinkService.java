package com.projectii.Nei.service;

import com.projectii.Nei.payload.response.TaskLinkDto;

import java.util.UUID;

public interface TaskLinkService {
    /** Strict: fails if either task already has a thread on that side, or if it would make a loop. */
    TaskLinkDto createLink(UUID fromId, UUID toId);

    /** What the Schedule canvas does when you drop a thread: replaces any existing thread out of "from" and into "to". */
    TaskLinkDto relink(UUID fromId, UUID toId);

    /** True if threading from -> to would close a loop (or from == to). */
    boolean wouldLoop(UUID fromId, UUID toId);

    void deleteLink(UUID linkId);

    void deleteLinkBetween(UUID fromId, UUID toId);
}
