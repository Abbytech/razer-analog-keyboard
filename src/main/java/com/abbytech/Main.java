package com.abbytech;

import com.abbytech.common.CompositeEvent;
import com.abbytech.protocol.Constants;
import com.abbytech.hid.Keyboard;
import com.abbytech.virtual.MappingInputDevice;
import org.apache.commons.codec.DecoderException;
import uk.co.bithatch.linuxio.EventCode;
import uk.co.bithatch.linuxio.InputDevice;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Main {
    private static InputDevice virtualKeyboard;
    private static MappingInputDevice mappingInputDevice;
    private static boolean joystickEnabled = false;

    public static void main(String[] args) throws DecoderException, IOException {
        Runtime.getRuntime().addShutdownHook(new Thread(Main::shutdown));

        virtualKeyboard = new InputDevice("Razer Keyboard", Constants.VENDOR_RAZER, Constants.HUNTSMAN_V3_PRO);
        virtualKeyboard.addCapability(Keyboard.getCapabilities().toArray(new EventCode[0]));
        virtualKeyboard.open();

        InputDevice gamepad = new InputDevice("Razer WASD", (short) 0x1234, (short) 0x5678);
        mappingInputDevice = new MappingInputDevice(gamepad);
        Keyboard.listen(Main::handleEvent);
    }

    public static void handleEvent(CompositeEvent compositeEvent) {
        boolean handled = enableDisableJoystickKeyComboCheck(compositeEvent);
        if (handled){
            return;
        }
        List<InputDevice.Event> originalEvents = compositeEvent.getEvents();

        if (joystickEnabled){
            Map<Boolean, List<InputDevice.Event>> collect = originalEvents.stream().collect(Collectors.partitioningBy(o -> mappingInputDevice.canHandle(o.getCode())));
            List<InputDevice.Event> keyboardEvents = collect.get(Boolean.FALSE);
            List<InputDevice.Event> gamepadEvents = collect.get(Boolean.TRUE);
            mappingInputDevice.handle(gamepadEvents);
            keyboardHandle(keyboardEvents);
        }else{
            keyboardHandle(originalEvents);
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

    private static boolean enableDisableJoystickKeyComboCheck(CompositeEvent compositeEvent) {
        boolean handled = false;
        List<InputDevice.Event> events = compositeEvent.getEvents();
        if (events.size() == 2) {
            InputDevice.Event event = events.get(0);
            InputDevice.Event event1 = events.get(1);
            EventCode eventCode = event.getCode();
            EventCode eventCode1 = event1.getCode();
            int value = event.getValue();
            int value1 = event1.getValue();

            if (eventCode.equals(EventCode.KEY_FN) && eventCode1.equals(EventCode.KEY_PAGEDOWN)) {
                if (value > Short.MAX_VALUE && value1 > Short.MAX_VALUE) {
                    if (joystickEnabled){
                        joystickEnabled = false;
                        System.out.println("joystick disabled");
                    }
                    handled = true;
                }
            } else if (eventCode.equals(EventCode.KEY_FN) && eventCode1.equals(EventCode.KEY_PAGEUP)) {
                if (value > Short.MAX_VALUE && value1 > Short.MAX_VALUE) {
                    if (!joystickEnabled){
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
            mappingInputDevice.close();
            virtualKeyboard.close();
        } catch (Exception e) {
            //ignored
            e.printStackTrace();
        }
    }
}