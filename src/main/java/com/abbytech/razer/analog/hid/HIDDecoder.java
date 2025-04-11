package com.abbytech.razer.analog.hid;

import com.abbytech.razer.analog.protocol.Constants;
import uk.co.bithatch.linuxio.EventCode;
import uk.co.bithatch.linuxio.InputDevice;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class HIDDecoder {
    private static final byte hidStartByte = 0x0b;
    private static final EventCode[] eventCodeMap = new EventCode[]{
            EventCode.KEY_RESERVED, EventCode.KEY_GRAVE, EventCode.KEY_1, EventCode.KEY_2, EventCode.KEY_3, EventCode.KEY_4, EventCode.KEY_5, EventCode.KEY_6, EventCode.KEY_7, EventCode.KEY_8, EventCode.KEY_9, EventCode.KEY_0, EventCode.KEY_MINUS, EventCode.KEY_EQUAL, EventCode.KEY_RESERVED, EventCode.KEY_BACKSPACE,//0x0f
            EventCode.KEY_TAB, EventCode.KEY_Q, EventCode.KEY_W, EventCode.KEY_E, EventCode.KEY_R, EventCode.KEY_T, EventCode.KEY_Y, EventCode.KEY_U, EventCode.KEY_I, EventCode.KEY_O, EventCode.KEY_P, EventCode.KEY_LEFTBRACE, EventCode.KEY_RIGHTBRACE, EventCode.KEY_BACKSLASH, EventCode.KEY_CAPSLOCK, EventCode.KEY_A,//0x1f
            EventCode.KEY_S, EventCode.KEY_D, EventCode.KEY_F, EventCode.KEY_G, EventCode.KEY_H, EventCode.KEY_J, EventCode.KEY_K, EventCode.KEY_L, EventCode.KEY_SEMICOLON, EventCode.KEY_APOSTROPHE, EventCode.KEY_RESERVED, EventCode.KEY_ENTER, EventCode.KEY_LEFTSHIFT, EventCode.KEY_RESERVED, EventCode.KEY_Z, EventCode.KEY_X,//0x2f
            EventCode.KEY_C, EventCode.KEY_V, EventCode.KEY_B, EventCode.KEY_N, EventCode.KEY_M, EventCode.KEY_COMMA, EventCode.KEY_DOT, EventCode.KEY_SLASH, EventCode.KEY_RESERVED, EventCode.KEY_RIGHTSHIFT, EventCode.KEY_LEFTCTRL, EventCode.KEY_FN, EventCode.KEY_LEFTALT, EventCode.KEY_SPACE, EventCode.KEY_RIGHTALT, EventCode.KEY_RESERVED,//0x3f
            EventCode.KEY_RIGHTCTRL, EventCode.KEY_RESERVED,EventCode.KEY_RESERVED, EventCode.KEY_RESERVED, EventCode.KEY_RESERVED, EventCode.KEY_RESERVED, EventCode.KEY_RESERVED, EventCode.KEY_RESERVED, EventCode.KEY_RESERVED, EventCode.KEY_RESERVED, EventCode.KEY_RESERVED, EventCode.KEY_INSERT, EventCode.KEY_DELETE, EventCode.KEY_RESERVED, EventCode.KEY_RESERVED, EventCode.KEY_LEFT,//0x4f
            EventCode.KEY_HOME, EventCode.KEY_END, EventCode.KEY_MEDIA, EventCode.KEY_UP, EventCode.KEY_DOWN, EventCode.KEY_PAGEUP, EventCode.KEY_PAGEDOWN, EventCode.KEY_RESERVED,EventCode.KEY_RESERVED,EventCode.KEY_RIGHT, EventCode.KEY_NUMLOCK, EventCode.KEY_KP7, EventCode.KEY_KP4, EventCode.KEY_KP1, EventCode.KEY_RESERVED, EventCode.KEY_KPSLASH, //0x5f
            EventCode.KEY_KP8, EventCode.KEY_KP5, EventCode.KEY_KP2, EventCode.KEY_KP0, EventCode.KEY_KPASTERISK, EventCode.KEY_KP9, EventCode.KEY_KP6, EventCode.KEY_KP3, EventCode.KEY_KPDOT, EventCode.KEY_KPMINUS, EventCode.KEY_KPPLUS, EventCode.KEY_RESERVED, EventCode.KEY_KPENTER, EventCode.KEY_RESERVED, EventCode.KEY_ESC, EventCode.KEY_RESERVED,//0x6f
            EventCode.KEY_F1, EventCode.KEY_F2, EventCode.KEY_F3, EventCode.KEY_F4, EventCode.KEY_F5, EventCode.KEY_F6, EventCode.KEY_F7, EventCode.KEY_F8, EventCode.KEY_F9, EventCode.KEY_F10, EventCode.KEY_F11, EventCode.KEY_F12, EventCode.KEY_PRINT, EventCode.KEY_SCROLLLOCK, EventCode.KEY_PAUSE, EventCode.KEY_LEFTMETA,//0x7f
            EventCode.KEY_RESERVED, EventCode.KEY_ROOT_MENU//0x81
    };

    private static final Collection<EventCode> capabilities;

    static {
        capabilities = Stream.of(eventCodeMap).filter(eventCode -> eventCode != EventCode.KEY_RESERVED).collect(Collectors.toList());
    }

    static Collection<EventCode> getCapabilities() {
        return capabilities;
    }

    private static List<HidPart> decode(ByteBuffer byteBuffer) {
        byte b = byteBuffer.get();
        if (b != hidStartByte) {
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

    public static List<InputDevice.Event> toInputDeviceEvent(ByteBuffer byteBuffer) {
        return HIDDecoder.decode(byteBuffer).stream().map(HIDDecoder::mapToInputDeviceEvent)
                .filter(eventKey -> !(eventKey.getCode().equals(EventCode.KEY_RESERVED) && eventKey.getValue() == 0))
                .collect(Collectors.toList());
    }

    private static InputDevice.Event mapToInputDeviceEvent(HidPart hidPart) {
        byte keyCode = hidPart.getKeyCode();
        int value = hidPart.getValue();
        int index = hidPart.keyCode & 0xFF;
        EventCode eventCode = eventCodeMap[index];
        if (eventCode.equals(EventCode.KEY_RESERVED) && keyCode != 0x00) {
            System.out.printf("UNMAPPED:%s%n", keyCode);
        }
        return new InputDevice.Event(eventCode, value);
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
