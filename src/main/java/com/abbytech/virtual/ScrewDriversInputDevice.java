package com.abbytech.virtual;

import com.abbytech.util.Util;
import uk.co.bithatch.linuxio.EventCode;
import uk.co.bithatch.linuxio.InputDevice;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class ScrewDriversInputDevice {
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
        input.add(EventCode.KEY_UP);
        input.add(EventCode.KEY_DOWN);
        input.add(EventCode.KEY_LEFT);
        input.add(EventCode.KEY_RIGHT);
    }

    public boolean isOpen(){
        return virtualInputDevice.isOpen();
    }

    public ScrewDriversInputDevice(InputDevice virtualInputDevice) throws IOException {
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
        return eventParts.stream().map(this::mapToInputEventGeneric).flatMap(Collection::stream)
                .collect(Collectors.groupingBy(InputDevice.Event::getCode)).values().stream()
                .map(events -> events.stream().reduce(new InputDevice.Event(null, 0), (event1, event2) -> {
                    int event1Value = event1.getValue();
                    int event2Value = event2.getValue();
                    int sum = event1Value + event2Value;

                    return new InputDevice.Event(event1.getCode() != null ? event1.getCode() : event2.getCode(), sum);
                })).collect(Collectors.toList());
    }

    private List<InputDevice.Event> mapToInputEventGeneric(InputDevice.Event event) {
        List<InputDevice.Event> mappedEvents = new ArrayList<>();
        EventCode mappedEventCode;
        int value = event.getValue();
        EventCode originalEventCode = event.getCode();
        switch (originalEventCode) {
            case KEY_W:
                mappedEventCode = EventCode.ABS_RZ;
                break;
            case KEY_S:
                mappedEventCode = EventCode.ABS_Z;
                break;
            case KEY_A:
            case KEY_D:
                mappedEventCode = EventCode.ABS_X;
                if (originalEventCode.equals(EventCode.KEY_A)) {
                    value = value * -1;
                }
                value /= 2;
                break;
            case KEY_Q:
                mappedEventCode = EventCode.BTN_TL;
                break;

            case KEY_ENTER:
                mappedEventCode = EventCode.BTN_X;
                break;

            case KEY_ESC:
                mappedEventCode = EventCode.BTN_B;
                break;
            case KEY_E:
                mappedEventCode = EventCode.BTN_TR;
                break;
            case KEY_RIGHT:
            case KEY_LEFT:
                mappedEventCode = EventCode.ABS_RX;
                if (originalEventCode.equals(EventCode.KEY_LEFT)) {
                    value = value * -1;
                }
                value /= 2;
                break;
            case KEY_UP:
            case KEY_DOWN:
                mappedEventCode = EventCode.ABS_RY;
                if (originalEventCode.equals(EventCode.KEY_DOWN)) {
                    value = value * -1;
                }
                value /= 2;
                break;

            default:
                throw new IllegalArgumentException("cannot handle EventCode " + originalEventCode);
        }

        int isButtonDown = Util.mapToBool(value);
        if (mappedEventCode.isButton()) {
            value = isButtonDown;
        }

        if (mappedEventCode == EventCode.ABS_Z) {
            mappedEvents.add(new InputDevice.Event(EventCode.BTN_TL2, Util.mapToBoolCalibrated(value)));
        } else if (mappedEventCode == EventCode.ABS_RZ) {
            mappedEvents.add(new InputDevice.Event(EventCode.BTN_TR2, Util.mapToBoolCalibrated(value)));
        }

        if (mappedEventCode == EventCode.ABS_RZ || mappedEventCode == EventCode.ABS_Z) {
            value -= Short.MAX_VALUE;
        }

        InputDevice.Event event1 = new InputDevice.Event(mappedEventCode, value);
        mappedEvents.add(event1);
        System.out.println(event1);

        return mappedEvents;
    }

    private void gamepadCapabilities() {
        virtualInputDevice.getCapabilities().add(EventCode.ABS_X);//wheel
        virtualInputDevice.getCapabilities().add(EventCode.ABS_Y);//ignored
        virtualInputDevice.getCapabilities().add(EventCode.ABS_Z);//left trigger/brake
        virtualInputDevice.getCapabilities().add(EventCode.ABS_RX);//ignored
        virtualInputDevice.getCapabilities().add(EventCode.ABS_RY);//ignored
        virtualInputDevice.getCapabilities().add(EventCode.ABS_RZ);//right trigger/accelerate
        virtualInputDevice.getCapabilities().add(EventCode.ABS_HAT0X);//ignored
        virtualInputDevice.getCapabilities().add(EventCode.ABS_HAT0Y);//ignored

        virtualInputDevice.getCapabilities().add(EventCode.BTN_A);
        virtualInputDevice.getCapabilities().add(EventCode.BTN_B);
        virtualInputDevice.getCapabilities().add(EventCode.BTN_X);
        virtualInputDevice.getCapabilities().add(EventCode.BTN_Y);
        virtualInputDevice.getCapabilities().add(EventCode.BTN_TL);
        virtualInputDevice.getCapabilities().add(EventCode.BTN_TR);
        virtualInputDevice.getCapabilities().add(EventCode.BTN_TL2);
        virtualInputDevice.getCapabilities().add(EventCode.BTN_TR2);
        virtualInputDevice.getCapabilities().add(EventCode.BTN_SELECT);
        virtualInputDevice.getCapabilities().add(EventCode.BTN_START);
        virtualInputDevice.getCapabilities().add(EventCode.BTN_MODE);
        virtualInputDevice.getCapabilities().add(EventCode.BTN_THUMBL);
        virtualInputDevice.getCapabilities().add(EventCode.BTN_THUMBR);
    }

    public void close() throws IOException {
        virtualInputDevice.close();

    }
}
