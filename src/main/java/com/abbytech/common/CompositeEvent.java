package com.abbytech.common;

import uk.co.bithatch.linuxio.InputDevice;

import java.util.List;

public class CompositeEvent {
    private final List<InputDevice.Event> events;

    public CompositeEvent(List<InputDevice.Event> events) {
        this.events = events;
    }

    public List<InputDevice.Event> getEvents() {
        return events;
    }

    @Override
    public String toString() {
        return "Event{" +
                "eventKeys=" + events +
                '}';
    }
}
