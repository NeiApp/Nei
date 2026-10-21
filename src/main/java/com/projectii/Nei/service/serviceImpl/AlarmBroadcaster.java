package com.projectii.Nei.service.serviceImpl;

import com.projectii.Nei.payload.utitityRecords.AlarmNotification;
import com.projectii.Nei.service.utilityService.NotificationSender;

import com.vaadin.flow.shared.Registration;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Component
public class AlarmBroadcaster implements NotificationSender {
    private final Set<Consumer<AlarmNotification>> listeners = ConcurrentHashMap.newKeySet();

    public Registration register(Consumer<AlarmNotification> l) { listeners.add(l); return () -> listeners.remove(l); }

    @Override public void send(AlarmNotification n) { listeners.forEach(l -> l.accept(n)); }
}
