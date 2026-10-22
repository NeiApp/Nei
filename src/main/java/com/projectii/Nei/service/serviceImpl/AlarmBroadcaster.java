package com.projectii.Nei.service.serviceImpl;

import com.projectii.Nei.payload.utitityRecords.AlarmNotification;
import com.projectii.Nei.service.utilityService.NotificationSender;

import com.vaadin.flow.shared.Registration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Component
@Slf4j
public class AlarmBroadcaster implements NotificationSender {
    private final Set<Consumer<AlarmNotification>> listeners = ConcurrentHashMap.newKeySet();

    public Registration register(Consumer<AlarmNotification> l) { listeners.add(l); return () -> listeners.remove(l); }

    public void send(AlarmNotification n) {
        log.info("ALARM: {}", n.message());      // shows in the IntelliJ console
        listeners.forEach(l -> l.accept(n));
    }
}
