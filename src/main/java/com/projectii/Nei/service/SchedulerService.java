package com.projectii.Nei.service;

import com.projectii.Nei.model.ScheduleRun;
import com.projectii.Nei.payload.response.RunStateDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
public interface SchedulerService {
    RunStateDto play(LocalDate date);

    RunStateDto finishCurrent(UUID runId);

    RunStateDto stop(UUID runId);

    void updatePosition(UUID taskId, double x, double y);

    @Transactional(readOnly = true)
    RunStateDto getState(UUID runId);

    RunStateDto restart(UUID runId);
}
