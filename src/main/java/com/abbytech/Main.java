package com.abbytech;

import org.apache.commons.codec.DecoderException;
import uk.co.bithatch.linuxio.EventCode;
import uk.co.bithatch.linuxio.InputDevice;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Main {
    private static InputDevice gamepad;
    private static InputDevice keyboard;

    public static void main(String[] args) throws DecoderException, IOException {
        Runtime.getRuntime().addShutdownHook(new Thread(Main::shutdown));

        gamepad = new InputDevice("Razer WASD", (short) 0x1234, (short) 0x5678);
        keyboard = new InputDevice("Razer Keyboard", Constants.VENDOR_RAZER, Constants.HUNTSMAN_V3_PRO);
        keyboard.addCapability(USB.getCapabilities().toArray(new EventCode[0]));
        keyboard.open();
        MappingInputDevice mappingInputDevice = new MappingInputDevice(gamepad);
        USB.listen(new USB.EventListener() {
            @Override
            public void onEvent(USB.CompositeEvent compositeEvent) {
                List<InputDevice.Event> originalEvents = compositeEvent.getEvents();
                Map<Boolean, List<InputDevice.Event>> collect = originalEvents.stream().collect(Collectors.partitioningBy(o -> mappingInputDevice.canHandle(o.getCode())));
                List<InputDevice.Event> keyboardEvents = collect.get(Boolean.FALSE);

                keyboardEvents.stream().map(event -> {
                    int value = event.getValue();
                    value = value > Short.MAX_VALUE ? 1 : 0;
                    return new InputDevice.Event(event.getCode(), value);
                }).forEach(event -> {
                    try {
                        keyboard.emit(event);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });

                List<InputDevice.Event> gamepadEvents = collect.get(Boolean.TRUE);
                mappingInputDevice.handle(gamepadEvents);
            }
        });
    }

    static class MappingInputDevice {
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

        MappingInputDevice(InputDevice virtualInputDevice) throws IOException {
            this.virtualInputDevice = virtualInputDevice;
            gamepadCapabilities();
            gamepad.open();
        }

        boolean canHandle(EventCode eventCode) {
            return false && input.contains(eventCode);
        }

        void handle(List<InputDevice.Event> events) {
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

    }


    private static void shutdown() {
        try {
            USB.reset();
            gamepad.close();
            keyboard.close();
            System.exit(0);
        } catch (Exception e) {
            //ignored
            e.printStackTrace();
        }
    }
}