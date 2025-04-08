package com.abbytech.hid;

import com.abbytech.common.CompositeEvent;
import com.abbytech.protocol.USB;
import org.apache.commons.codec.DecoderException;
import uk.co.bithatch.linuxio.EventCode;
import uk.co.bithatch.linuxio.InputDevice;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.List;

public class Keyboard {
    public interface EventListener {
        void onEvent(CompositeEvent event);
    }

    public static void listen(EventListener listener) throws DecoderException {
        USB.openDevice();
        while (true) {
            ByteBuffer byteBuffer = USB.readHIDData();
            CompositeEvent compositeEvent = HIDDecoder.decode(byteBuffer);
            if (shouldTerminate(compositeEvent)) {
                break;
            } else {
                listener.onEvent(compositeEvent);
            }
        }
    }

    private static boolean shouldTerminate(CompositeEvent compositeEvent) {
        List<InputDevice.Event> events = compositeEvent.getEvents();
        if (events.size() == 2) {
            InputDevice.Event event = events.get(0);
            InputDevice.Event event1 = events.get(1);
            EventCode eventCode = event.getCode();
            EventCode eventCode1 = event1.getCode();
            int value = event.getValue();
            int value1 = event1.getValue();

            if (eventCode.equals(EventCode.KEY_FN) && eventCode1.equals(EventCode.KEY_HOME)) {
                if (value > Short.MAX_VALUE && value1 > Short.MAX_VALUE) {
                    System.out.println("terminating");
                    return true;
                }
            }
        }
        return false;
    }


    public static Collection<EventCode> getCapabilities() {
        return HIDDecoder.getCapabilities();
    }
}
