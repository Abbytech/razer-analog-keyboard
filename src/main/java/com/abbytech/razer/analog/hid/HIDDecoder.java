package com.abbytech.razer.analog.hid;

import com.abbytech.razer.analog.layout.Layout;
import com.abbytech.razer.analog.protocol.Constants;
import uk.co.bithatch.linuxio.EventCode;
import uk.co.bithatch.linuxio.InputDevice;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.stream.Collectors;

public class HIDDecoder {
    private final Layout layout;

    public HIDDecoder(Layout layout) {
        this.layout = layout;
    }

    public List<InputDevice.Event> toInputDeviceEvent(ByteBuffer byteBuffer) {
        return HIDDecoder.decode(byteBuffer).stream().map(this::mapToInputDeviceEvent)
                .filter(eventKey -> !(eventKey.getCode().equals(EventCode.KEY_RESERVED) && eventKey.getValue() == 0))
                .collect(Collectors.toList());
    }

    private InputDevice.Event mapToInputDeviceEvent(HidPart hidPart) {
        byte keyCode = hidPart.getKeyCode();
        int value = hidPart.getValue();
        int index = hidPart.keyCode & 0xFF;
        EventCode eventCode = layout.getEventCodeMap()[index];
        if (eventCode.equals(EventCode.KEY_RESERVED) && keyCode != 0x00) {
            System.out.printf("UNMAPPED:%s%n", keyCode);
        }
        return new InputDevice.Event(eventCode, value);
    }

    private static List<HidPart> decode(ByteBuffer byteBuffer) {
        byte b = byteBuffer.get();
        if (b != Constants.hidStartByte) {
            System.err.printf("unexpected start byte %d%n", b);
            return Collections.emptyList();
        }
        List<HidPart> hidPartList = new ArrayList<>();
        for (int i = 1; i < Constants.HID_DATA_LENGTH - 2; i += 3) {
            byte keyCode = byteBuffer.get();
            if (keyCode == 0x00)
                break;
            short value = byteBuffer.getShort();
            int unsignedValue = value & 0xFFFF;
            hidPartList.add(new HidPart(keyCode, unsignedValue));
        }
        return hidPartList;
    }

    private static class HidPart {
        private final byte keyCode;
        private final int value;

        private HidPart(Byte keyCode, Integer value) {
            this.keyCode = keyCode;
            this.value = value;
        }

        public byte getKeyCode() {
            return keyCode;
        }

        public int getValue() {
            return value;
        }
    }
}
