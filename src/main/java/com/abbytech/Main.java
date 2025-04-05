package com.abbytech;

import org.apache.commons.codec.DecoderException;
import uk.co.bithatch.linuxio.EventCode;
import uk.co.bithatch.linuxio.InputDevice;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws DecoderException, IOException {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                USB.reset();
            } catch (DecoderException e) {
                throw new RuntimeException(e);
            }
        }));

        InputDevice dev = new InputDevice("Razer WASD", (short) 0x1234, (short) 0x5678);

        /*dev.getCapabilities().add(EventCode.ABS_GAS);
        dev.getCapabilities().add(EventCode.ABS_BRAKE);
        dev.getCapabilities().add(EventCode.ABS_WHEEL);*/
        dev.getCapabilities().add(EventCode.ABS_Y);
        dev.getCapabilities().add(EventCode.ABS_X);
        dev.open();
        USB.listen(event -> {
            String key = event.getKey();
            Integer value = event.getValue();
            if (key == null) {
                return;
            }
            EventCode eventCode;
            switch (key) {
                case "W":
                case "S":
                    eventCode = EventCode.ABS_Y;
                    if (key.equals("S")) {
                        value = value * -1;
                    }
                    break;
                case "A":
                case "D":
                    eventCode = EventCode.ABS_X;
                    if (key.equals("A")) {
                        value = value * -1;
                    }
                    break;
                default:
                    System.out.println("unknown key");
                    return;
            }
            try {
                dev.emit(new InputDevice.Event(eventCode, value));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        dev.close();
    }
}