package com.abbytech.razer.analog.hid;

import org.apache.commons.codec.binary.Hex;
import uk.co.bithatch.linuxio.EventCode;
import uk.co.bithatch.linuxio.InputDevice;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.stream.Collectors;

class HIDDecoder {
    private static final Map<String, EventCode> keyCodeMap = new HashMap<>();
    private static final int initialCapacity = 15;

    static {
        keyCodeMap.put("01", EventCode.KEY_GRAVE);
        //horizontal numbers
        keyCodeMap.put("02", EventCode.KEY_1);
        keyCodeMap.put("03", EventCode.KEY_2);
        keyCodeMap.put("04", EventCode.KEY_3);
        keyCodeMap.put("05", EventCode.KEY_4);
        keyCodeMap.put("06", EventCode.KEY_5);
        keyCodeMap.put("07", EventCode.KEY_6);
        keyCodeMap.put("08", EventCode.KEY_7);
        keyCodeMap.put("09", EventCode.KEY_8);
        keyCodeMap.put("0a", EventCode.KEY_9);
        keyCodeMap.put("0b", EventCode.KEY_0);
        keyCodeMap.put("0c", EventCode.KEY_MINUS);
        keyCodeMap.put("0d", EventCode.KEY_EQUAL);
        keyCodeMap.put("0e", EventCode.KEY_RESERVED);
        keyCodeMap.put("0f", EventCode.KEY_BACKSPACE);
        keyCodeMap.put("10", EventCode.KEY_TAB);
        keyCodeMap.put("11", EventCode.KEY_Q);
        keyCodeMap.put("12", EventCode.KEY_W);
        keyCodeMap.put("13", EventCode.KEY_E);
        keyCodeMap.put("14", EventCode.KEY_R);
        keyCodeMap.put("15", EventCode.KEY_T);
        keyCodeMap.put("16", EventCode.KEY_Y);
        keyCodeMap.put("17", EventCode.KEY_U);
        keyCodeMap.put("18", EventCode.KEY_I);
        keyCodeMap.put("19", EventCode.KEY_O);
        keyCodeMap.put("1a", EventCode.KEY_P);
        keyCodeMap.put("1b", EventCode.KEY_LEFTBRACE);
        keyCodeMap.put("1c", EventCode.KEY_RIGHTBRACE);
        keyCodeMap.put("1d", EventCode.KEY_BACKSLASH);
        keyCodeMap.put("1e", EventCode.KEY_CAPSLOCK);
        keyCodeMap.put("1f", EventCode.KEY_A);
        keyCodeMap.put("20", EventCode.KEY_S);
        keyCodeMap.put("21", EventCode.KEY_D);
        keyCodeMap.put("22", EventCode.KEY_F);
        keyCodeMap.put("23", EventCode.KEY_G);
        keyCodeMap.put("24", EventCode.KEY_H);
        keyCodeMap.put("25", EventCode.KEY_J);
        keyCodeMap.put("26", EventCode.KEY_K);
        keyCodeMap.put("27", EventCode.KEY_L);
        keyCodeMap.put("28", EventCode.KEY_SEMICOLON);
        keyCodeMap.put("29", EventCode.KEY_APOSTROPHE);
        keyCodeMap.put("2a", EventCode.KEY_RESERVED);
        keyCodeMap.put("2b", EventCode.KEY_ENTER);
        keyCodeMap.put("2c", EventCode.KEY_LEFTSHIFT);
        keyCodeMap.put("2e", EventCode.KEY_Z);
        keyCodeMap.put("2f", EventCode.KEY_X);
        keyCodeMap.put("30", EventCode.KEY_C);
        keyCodeMap.put("31", EventCode.KEY_V);
        keyCodeMap.put("32", EventCode.KEY_B);
        keyCodeMap.put("33", EventCode.KEY_N);
        keyCodeMap.put("34", EventCode.KEY_M);
        keyCodeMap.put("35", EventCode.KEY_COMMA);
        keyCodeMap.put("36", EventCode.KEY_DOT);
        keyCodeMap.put("37", EventCode.KEY_SLASH);
        keyCodeMap.put("38", EventCode.KEY_RESERVED);
        keyCodeMap.put("39", EventCode.KEY_RIGHTSHIFT);
        keyCodeMap.put("3a", EventCode.KEY_LEFTCTRL);
        keyCodeMap.put("3b", EventCode.KEY_FN);
        keyCodeMap.put("3c", EventCode.KEY_LEFTALT);
        keyCodeMap.put("3d", EventCode.KEY_SPACE);
        keyCodeMap.put("3e", EventCode.KEY_RIGHTALT);
        keyCodeMap.put("40", EventCode.KEY_RIGHTCTRL);

        //keypad/numpad begin
        keyCodeMap.put("5a", EventCode.KEY_NUMLOCK);
        keyCodeMap.put("5b", EventCode.KEY_KP7);
        keyCodeMap.put("5c", EventCode.KEY_KP4);
        keyCodeMap.put("5d", EventCode.KEY_KP1);
        keyCodeMap.put("5f", EventCode.KEY_KPSLASH);
        keyCodeMap.put("60", EventCode.KEY_KP8);
        keyCodeMap.put("61", EventCode.KEY_KP5);
        keyCodeMap.put("62", EventCode.KEY_KP2);
        keyCodeMap.put("63", EventCode.KEY_KP0);
        keyCodeMap.put("64", EventCode.KEY_KPASTERISK);
        keyCodeMap.put("65", EventCode.KEY_KP9);
        keyCodeMap.put("66", EventCode.KEY_KP6);
        keyCodeMap.put("67", EventCode.KEY_KP3);
        keyCodeMap.put("68", EventCode.KEY_KPDOT);
        keyCodeMap.put("69", EventCode.KEY_MINUS);
        keyCodeMap.put("6a", EventCode.KEY_KPPLUS);
        keyCodeMap.put("6c", EventCode.KEY_KPENTER);
        keyCodeMap.put("6e", EventCode.KEY_ESC);
        //keypad/numpad end

        keyCodeMap.put("4f", EventCode.KEY_LEFT);
        keyCodeMap.put("53", EventCode.KEY_UP);
        keyCodeMap.put("54", EventCode.KEY_DOWN);
        keyCodeMap.put("59", EventCode.KEY_RIGHT);
        keyCodeMap.put("4b", EventCode.KEY_INSERT);
        keyCodeMap.put("4c", EventCode.KEY_DELETE);
        keyCodeMap.put("50", EventCode.KEY_HOME);
        keyCodeMap.put("51", EventCode.KEY_END);
        keyCodeMap.put("55", EventCode.KEY_PAGEUP);
        keyCodeMap.put("56", EventCode.KEY_PAGEDOWN);
        keyCodeMap.put("81", EventCode.KEY_CONTEXT_MENU);
        keyCodeMap.put("70", EventCode.KEY_F1);
        keyCodeMap.put("71", EventCode.KEY_F2);
        keyCodeMap.put("72", EventCode.KEY_F3);
        keyCodeMap.put("73", EventCode.KEY_F4);
        keyCodeMap.put("74", EventCode.KEY_F5);
        keyCodeMap.put("75", EventCode.KEY_F6);
        keyCodeMap.put("76", EventCode.KEY_F7);
        keyCodeMap.put("77", EventCode.KEY_F8);
        keyCodeMap.put("78", EventCode.KEY_F9);
        keyCodeMap.put("79", EventCode.KEY_F10);
        keyCodeMap.put("7a", EventCode.KEY_F11);
        keyCodeMap.put("7b", EventCode.KEY_F12);
        keyCodeMap.put("7c", EventCode.KEY_PRINT);
        keyCodeMap.put("7d", EventCode.KEY_SCROLLLOCK);
        keyCodeMap.put("7e", EventCode.KEY_PAUSE);
        keyCodeMap.put("7f", EventCode.KEY_LEFTMETA);
        keyCodeMap.put("00", EventCode.KEY_RESERVED);

        keyCodeMap.put("52", EventCode.KEY_MEDIA);
    }

    static Collection<EventCode> getCapabilities() {
        return keyCodeMap.values();
    }

    private static List<HidPart> decode(String hexString) {
        String hidParts = hexString.substring(2);
        List<HidPart> hidPartList = new ArrayList<>(initialCapacity);
        for (int i = 0; i < 15; i++) {
            int offset = i * 6;
            String hidPart = hidParts.substring(offset, offset + 6);
            String keyCode = hidPart.substring(0, 2);
            Integer value = Integer.decode("0x" + hidPart.substring(2, 6));
            hidPartList.add(new HidPart(keyCode, value));
        }
        return hidPartList;
    }

    public static List<InputDevice.Event> decode(ByteBuffer byteBuffer) {
        String hidData = new String(Hex.encodeHex(byteBuffer));
        return HIDDecoder.decode(hidData).stream().map(hidPart -> {
                    String keyCode = hidPart.getKeyCode();
                    Integer value = hidPart.getValue();
                    EventCode eventCode = Optional.ofNullable(keyCodeMap.get(keyCode)).orElse(EventCode.KEY_RESERVED);
                    if (eventCode.equals(EventCode.KEY_RESERVED) && !keyCode.equals("00")) {
                        System.out.printf("UNMAPPED:%s%n", keyCode);
                    }
                    return new InputDevice.Event(eventCode, value);
                }).filter(eventKey -> !(eventKey.getCode().equals(EventCode.KEY_RESERVED) && eventKey.getValue() == 0))
                .collect(Collectors.toList());
    }

    private static class HidPart {
        private final String keyCode;
        private final Integer value;

        private HidPart(String keyCode, Integer value) {
            this.keyCode = keyCode;
            this.value = value;
        }

        public String getKeyCode() {
            return keyCode;
        }

        public Integer getValue() {
            return value;
        }
    }
}
