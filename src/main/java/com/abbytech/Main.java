package com.abbytech;

import org.apache.commons.codec.DecoderException;
import uk.co.bithatch.linuxio.EventCode;
import uk.co.bithatch.linuxio.InputDevice;

import java.io.IOException;
import java.util.List;

public class Main {
    private static InputDevice virtualInputDevice;

    public static void main(String[] args) throws DecoderException, IOException {
        Runtime.getRuntime().addShutdownHook(new Thread(Main::shutdown));

        virtualInputDevice = new InputDevice("Razer WASD", (short) 0x1234, (short) 0x5678);

        /*dev.getCapabilities().add(EventCode.ABS_GAS);
        dev.getCapabilities().add(EventCode.ABS_BRAKE);
        dev.getCapabilities().add(EventCode.ABS_WHEEL);*/
        virtualInputDevice.getCapabilities().add(EventCode.ABS_Y);
        virtualInputDevice.getCapabilities().add(EventCode.ABS_X);
        virtualInputDevice.open();
        USB.listen(event -> {
            List<USB.EventKey> eventParts = event.getEventParts();
            if (eventParts.size() == 1) {
                USB.EventKey eventKey = eventParts.get(0);
                USB.Key key = eventKey.getKey();
                Integer value = eventKey.getValue();
                if (key == null) {
                    return;
                }
                EventCode eventCode;
                switch (key) {
                    case W:
                    case S:
                        eventCode = EventCode.ABS_Y;
                        if (key.equals(USB.Key.S)) {
                            value = value * -1;
                        }
                        break;
                    case A:
                    case D:
                        eventCode = EventCode.ABS_X;
                        if (key.equals(USB.Key.A)) {
                            value = value * -1;
                        }
                        break;
                    default:
                        System.out.println("unknown key");
                        return;
                }
                try {
                    virtualInputDevice.emit(new InputDevice.Event(eventCode, value));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

        });
    }

    private static void shutdown() {
        try {
            USB.reset();
            virtualInputDevice.close();
            System.exit(0);
        } catch (Exception e) {
            //ignored
            e.printStackTrace();
        }
    }
}