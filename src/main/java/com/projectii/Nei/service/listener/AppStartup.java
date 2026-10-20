package com.projectii.Nei.service.listener;

import com.projectii.Nei.service.serviceImpl.WeekService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AppStartup implements ApplicationListener<ApplicationReadyEvent> {

    private final WeekService weekService;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        weekService.initializeFirstWeek();
    }
}
