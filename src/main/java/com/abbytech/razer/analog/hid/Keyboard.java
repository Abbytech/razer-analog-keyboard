package com.abbytech.razer.analog.hid;

import com.abbytech.razer.analog.layout.Layout;
import com.abbytech.razer.analog.protocol.USB;
import uk.co.bithatch.linuxio.EventCode;
import uk.co.bithatch.linuxio.InputDevice;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.List;

import static com.abbytech.razer.analog.util.Util.halfwayDown;

public class Keyboard {
    private final USB usb;
    private final HIDDecoder decoder;
    private final Layout layout;

    public Keyboard(USB usb, Layout layout) {
        this.usb = usb;
        this.layout = layout;
        this.decoder = new HIDDecoder(layout);
    }

    public void listen(EventListener listener) {
        usb.openDevice();
        while (true) {
            ByteBuffer byteBuffer;
            synchronized (usb) {
                if (usb.isDeviceOpen())
                    byteBuffer = usb.readHIDData();
                else
                    break;
            }
            List<InputDevice.Event> events = decoder.toInputDeviceEvent(byteBuffer);
            if (shouldTerminate(events)) {
                closeIfOpen();
                break;
            } else {
                listener.onEvent(events);
            }
        }
    }

    public void closeIfOpen() {
        synchronized (usb) {
            if (usb.isDeviceOpen()){
                try {
                    usb.closeDevice();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private boolean shouldTerminate(List<InputDevice.Event> events) {
        if (events.size() == 2) {
            InputDevice.Event event = events.get(0);
            InputDevice.Event event1 = events.get(1);
            EventCode eventCode = event.getCode();
            EventCode eventCode1 = event1.getCode();
            int value = event.getValue();
            int value1 = event1.getValue();

            if (eventCode.equals(EventCode.KEY_FN) && eventCode1.equals(EventCode.KEY_HOME)) {
                if (value > halfwayDown && value1 > halfwayDown) {
                    System.out.println("terminating");
                    return true;
                }
            }
        }
        return false;
    }

    public Collection<EventCode> getCapabilities() {
        return layout.getCapabilities();
    }

    public interface EventListener {
        void onEvent(List<InputDevice.Event> event);
    }
}
