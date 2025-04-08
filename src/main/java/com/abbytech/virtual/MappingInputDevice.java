package com.abbytech.virtual;

import uk.co.bithatch.linuxio.EventCode;
import uk.co.bithatch.linuxio.InputDevice;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MappingInputDevice {
    private final InputDevice virtualInputDevice;
    private final List<EventCode> input = new ArrayList<>();

    {
        input.add(EventCode.KEY_W);
        input.add(EventCode.KEY_S);
        input.add(EventCode.KEY_A);
        input.add(EventCode.KEY_D);
        input.add(EventCode.KEY_Q);
        input.add(EventCode.KEY_ENTER);
        input.add(EventCode.KEY_ESC);
        input.add(EventCode.KEY_E);
    }

    public MappingInputDevice(InputDevice virtualInputDevice) throws IOException {
        this.virtualInputDevice = virtualInputDevice;
        gamepadCapabilities();
        this.virtualInputDevice.open();
    }

    public boolean canHandle(EventCode eventCode) {
        return input.contains(eventCode);
    }

    public void handle(List<InputDevice.Event> events) {
        List<InputDevice.Event> mappedEvents = mapToInputDeviceEvents(events);
        mappedEvents.forEach(inputDeviceEvent -> {
            try {
                virtualInputDevice.emit(inputDeviceEvent, false);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        if (!mappedEvents.isEmpty()) {
            try {
                virtualInputDevice.syn();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private List<InputDevice.Event> mapToInputDeviceEvents(List<InputDevice.Event> eventParts) {
        return eventParts.stream().map(this::mapToInputEventGeneric)
                .collect(Collectors.groupingBy(InputDevice.Event::getCode)).values().stream()
                .map(events -> events.stream().reduce(new InputDevice.Event(null, 0), (event1, event2) -> {
                    int event1Value = event1.getValue();
                    int event2Value = event2.getValue();
                    int sum = event1Value + event2Value;

                    return new InputDevice.Event(event1.getCode() != null ? event1.getCode() : event2.getCode(), sum);
                })).collect(Collectors.toList());
    }

    private InputDevice.Event mapToInputEventGeneric(InputDevice.Event event) {
        EventCode mappedEventCode;
        int value = event.getValue();
        EventCode originalEventCode = event.getCode();
        switch (originalEventCode) {
            case KEY_W:
            case KEY_S:
                mappedEventCode = EventCode.ABS_Y;
                if (originalEventCode.equals(EventCode.KEY_W)) {
                    value = value * -1;
                }
                break;
            case KEY_A:
            case KEY_D:
                mappedEventCode = EventCode.ABS_X;
                if (originalEventCode.equals(EventCode.KEY_A)) {
                    value = value * -1;
                }
                break;
            case KEY_Q:
                mappedEventCode = EventCode.BTN_TL;
                break;

            case KEY_ENTER:
                mappedEventCode = EventCode.BTN_A;
                break;

            case KEY_ESC:
                mappedEventCode = EventCode.BTN_X;
                break;
            case KEY_E:
                mappedEventCode = EventCode.BTN_TR;
                break;

            default:
                throw new IllegalArgumentException("cannot handle EventCode " + originalEventCode);
        }

        if (mappedEventCode.isButton()) {
            value = value > Short.MAX_VALUE ? 1 : 0;
        }
        return new InputDevice.Event(mappedEventCode, value);
    }

    private void gamepadCapabilities() {
        virtualInputDevice.getCapabilities().add(EventCode.ABS_X);
        virtualInputDevice.getCapabilities().add(EventCode.ABS_Y);
        virtualInputDevice.getCapabilities().add(EventCode.BTN_TR);
        virtualInputDevice.getCapabilities().add(EventCode.BTN_TL);
        virtualInputDevice.getCapabilities().add(EventCode.BTN_A);
        virtualInputDevice.getCapabilities().add(EventCode.BTN_X);
    }

    public void close() throws IOException {
        virtualInputDevice.close();

    }
}
