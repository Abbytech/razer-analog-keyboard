package com.abbytech.razer.analog;

import com.abbytech.razer.analog.config.InputDeviceConfig;
import com.abbytech.razer.analog.layout.HuntsmanV3ProLayout;
import com.abbytech.razer.analog.protocol.Constants;
import com.abbytech.razer.analog.hid.Keyboard;
import com.abbytech.razer.analog.protocol.USB;
import com.abbytech.razer.analog.virtual.ConfigurableInputDevice;
import com.abbytech.razer.analog.virtual.GenericDevice;
import com.fasterxml.jackson.databind.ObjectMapper;
import uk.co.bithatch.linuxio.EventCode;
import uk.co.bithatch.linuxio.InputDevice;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.abbytech.razer.analog.protocol.Constants.HUNTSMAN_V3_PRO;

public class Main {
    private static InputDevice virtualKeyboard;
    private static GenericDevice mappingInputDevice;
    private static boolean joystickEnabled = false;

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.out.println("usage: java -jar <jar-name.jar> <device-config-file-path>");
        }
        String outputDeviceConfigJsonFile = args[0];
        File file = new File(outputDeviceConfigJsonFile);
        InputDeviceConfig inputDeviceConfig = new ObjectMapper().readValue(file, InputDeviceConfig.class);

        Runtime.getRuntime().addShutdownHook(new Thread(Main::shutdown));
        Keyboard keyboard = new Keyboard(new USB(HUNTSMAN_V3_PRO), new HuntsmanV3ProLayout());
        virtualKeyboard = createVirtualKeyboard(keyboard);
        InputDevice gamepad = new InputDevice(inputDeviceConfig.getDeviceName(), inputDeviceConfig.getVendorId(), inputDeviceConfig.getProductId());
        mappingInputDevice = new ConfigurableInputDevice(gamepad, inputDeviceConfig.getInputOutputMapping(), inputDeviceConfig.getDefaultActuationPoint());
        keyboard.listen(Main::handleEvent);
        virtualKeyboard.close();
    }

    private static InputDevice createVirtualKeyboard(Keyboard keyboard) throws IOException {
        virtualKeyboard = new InputDevice("Razer Keyboard", Constants.VENDOR_RAZER, HUNTSMAN_V3_PRO);
        virtualKeyboard.addCapability(keyboard.getCapabilities().toArray(new EventCode[0]));
        virtualKeyboard.open();
        return virtualKeyboard;
    }

    public static void handleEvent(List<InputDevice.Event> events) {
        boolean handled = enableDisableJoystickKeyComboCheck(events);
        if (handled) {
            return;
        }

        if (joystickEnabled) {
            if (!mappingInputDevice.isOpen()) {
                try {
                    mappingInputDevice.open();
                    System.out.println("mapping input device opened");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
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