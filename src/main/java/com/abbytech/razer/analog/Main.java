package com.abbytech.razer.analog;

import com.abbytech.razer.analog.protocol.Constants;
import com.abbytech.razer.analog.hid.Keyboard;
import com.abbytech.razer.analog.protocol.USB;
import com.abbytech.razer.analog.virtual.ScrewDriversInputDevice;
import uk.co.bithatch.linuxio.EventCode;
import uk.co.bithatch.linuxio.InputDevice;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class Main {
    private static InputDevice virtualKeyboard;
    private static ScrewDriversInputDevice mappingInputDevice;
    private static boolean joystickEnabled = false;

    public static void main(String[] args) throws IOException {
        Runtime.getRuntime().addShutdownHook(new Thread(Main::shutdown));
        Keyboard keyboard = new Keyboard(new USB());
        virtualKeyboard = createVirtualKeyboard(keyboard);
        InputDevice gamepad = new InputDevice("Razer WASD", (short) 0x1234, (short) 0x5678);
        mappingInputDevice = new ScrewDriversInputDevice(gamepad);
        keyboard.listen(Main::handleEvent);
        virtualKeyboard.close();
    }

    private static boolean findPhysicalKeyboardAndWaitForEnableCombo(Keyboard keyboard) throws IOException {
        Optional<InputDevice> inputDevice = getActualKeyboard();
        if (inputDevice.isPresent()) {
            InputDevice actualKeyboard = inputDevice.get();
            actualKeyboard.open();
            waitForEnableCombo(actualKeyboard);
            virtualKeyboard = createVirtualKeyboard(keyboard);
        } else {
            System.err.println("original keyboard not found; exiting...");
            return true;
        }
        return false;
    }

    private static InputDevice createVirtualKeyboard(Keyboard keyboard) throws IOException {
        virtualKeyboard = new InputDevice("Razer Keyboard", Constants.VENDOR_RAZER, Constants.HUNTSMAN_V3_PRO);
        virtualKeyboard.addCapability(keyboard.getCapabilities().toArray(new EventCode[0]));
        virtualKeyboard.open();
        return virtualKeyboard;
    }

    private static void waitForEnableCombo(InputDevice actualKeyboard) throws IOException {
        System.out.println("waiting for enable command");
        while (true) {
            InputDevice.Event event = actualKeyboard.nextEvent();
            if (event.getCode().equals(EventCode.KEY_FN) && event.getValue() == 1) {
                InputDevice.Event event2 = actualKeyboard.nextEvent();
                if (event2.getCode().equals(EventCode.KEY_HOME) && event.getValue() == 1) {
                    System.out.println("got enable command");
                    break;
                }
            }
        }
    }

    private static Optional<InputDevice> getActualKeyboard() throws IOException {
        List<InputDevice> devices = InputDevice.getAllKeyboardDevices().stream()
                .filter(inputDevice -> inputDevice.getVendor() == Constants.VENDOR_RAZER && inputDevice.getProduct() == Constants.HUNTSMAN_V3_PRO)
                .filter(inputDevice -> {
                    System.out.println();
                    return /*inputDevice.getCapabilities(EventCode.Type.EV_KEY).contains(EventCode.KEY_FN) && */inputDevice.getCapabilities(EventCode.Type.EV_KEY).contains(EventCode.KEY_HOME);
                })
                .collect(Collectors.toList());

        if (devices.isEmpty()) {
            return Optional.empty();
        } else {
            if (devices.size() > 1) {
                System.err.println("found more than one physical keyboard" + devices);
            }
            return Optional.of(devices.get(1));
        }
    }

    public static void handleEvent(List<InputDevice.Event> events) {
        boolean handled = enableDisableJoystickKeyComboCheck(events);
        if (handled) {
            return;
        }

        if (joystickEnabled) {
            Map<Boolean, List<InputDevice.Event>> collect = events.stream().collect(Collectors.partitioningBy(o -> mappingInputDevice.canHandle(o.getCode())));
            List<InputDevice.Event> keyboardEvents = collect.get(Boolean.FALSE);
            List<InputDevice.Event> gamepadEvents = collect.get(Boolean.TRUE);
            mappingInputDevice.handle(gamepadEvents);
            keyboardHandle(keyboardEvents);
        } else {
            keyboardHandle(events);
        }
    }

    private static void keyboardHandle(List<InputDevice.Event> keyboardEvents) {
        keyboardEvents.stream().map(event -> {
            int value = event.getValue();
            value = value > Short.MAX_VALUE ? 1 : 0;
            return new InputDevice.Event(event.getCode(), value);
        }).forEach(event -> {
            try {
                virtualKeyboard.emit(event);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static boolean enableDisableJoystickKeyComboCheck(List<InputDevice.Event> events) {
        boolean handled = false;
        if (events.size() == 2) {
            InputDevice.Event event = events.get(0);
            InputDevice.Event event1 = events.get(1);
            EventCode eventCode = event.getCode();
            EventCode eventCode1 = event1.getCode();
            int value = event.getValue();
            int value1 = event1.getValue();

            if (eventCode.equals(EventCode.KEY_FN) && eventCode1.equals(EventCode.KEY_PAGEDOWN)) {
                if (value > Short.MAX_VALUE && value1 > Short.MAX_VALUE) {
                    if (joystickEnabled) {
                        joystickEnabled = false;
                        System.out.println("joystick disabled");
                    }
                    handled = true;
                }
            } else if (eventCode.equals(EventCode.KEY_FN) && eventCode1.equals(EventCode.KEY_PAGEUP)) {
                if (value > Short.MAX_VALUE && value1 > Short.MAX_VALUE) {
                    if (!joystickEnabled) {
                        joystickEnabled = true;
                        System.out.println("joystick enabled");
                    }
                    handled = true;
                }
            }
        }
        return handled;
    }

    private static void shutdown() {
        try {
            if (mappingInputDevice.isOpen())
                mappingInputDevice.close();
            if (virtualKeyboard.isOpen())
                virtualKeyboard.close();
        } catch (Exception e) {
            //ignored
            e.printStackTrace();
        }
    }
}