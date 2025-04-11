package com.abbytech.razer.analog.layout;

import uk.co.bithatch.linuxio.EventCode;

public class HuntsmanV3ProLayout implements Layout{
    private static final EventCode[] eventCodeMap = new EventCode[]{
            /*0x00*/EventCode.KEY_RESERVED, EventCode.KEY_GRAVE, EventCode.KEY_1, EventCode.KEY_2, EventCode.KEY_3, EventCode.KEY_4, EventCode.KEY_5, EventCode.KEY_6, EventCode.KEY_7, EventCode.KEY_8, EventCode.KEY_9, EventCode.KEY_0, EventCode.KEY_MINUS, EventCode.KEY_EQUAL, EventCode.KEY_RESERVED, EventCode.KEY_BACKSPACE,//0x0f
            /*0x10*/EventCode.KEY_TAB, EventCode.KEY_Q, EventCode.KEY_W, EventCode.KEY_E, EventCode.KEY_R, EventCode.KEY_T, EventCode.KEY_Y, EventCode.KEY_U, EventCode.KEY_I, EventCode.KEY_O, EventCode.KEY_P, EventCode.KEY_LEFTBRACE, EventCode.KEY_RIGHTBRACE, EventCode.KEY_BACKSLASH, EventCode.KEY_CAPSLOCK, EventCode.KEY_A,//0x1f
            /*0x20*/EventCode.KEY_S, EventCode.KEY_D, EventCode.KEY_F, EventCode.KEY_G, EventCode.KEY_H, EventCode.KEY_J, EventCode.KEY_K, EventCode.KEY_L, EventCode.KEY_SEMICOLON, EventCode.KEY_APOSTROPHE, EventCode.KEY_RESERVED, EventCode.KEY_ENTER, EventCode.KEY_LEFTSHIFT, EventCode.KEY_RESERVED, EventCode.KEY_Z, EventCode.KEY_X,//0x2f
            /*0x30*/EventCode.KEY_C, EventCode.KEY_V, EventCode.KEY_B, EventCode.KEY_N, EventCode.KEY_M, EventCode.KEY_COMMA, EventCode.KEY_DOT, EventCode.KEY_SLASH, EventCode.KEY_RESERVED, EventCode.KEY_RIGHTSHIFT, EventCode.KEY_LEFTCTRL, EventCode.KEY_FN, EventCode.KEY_LEFTALT, EventCode.KEY_SPACE, EventCode.KEY_RIGHTALT, EventCode.KEY_RESERVED,//0x3f
            /*0x40*/EventCode.KEY_RIGHTCTRL, EventCode.KEY_RESERVED,EventCode.KEY_RESERVED, EventCode.KEY_RESERVED, EventCode.KEY_RESERVED, EventCode.KEY_RESERVED, EventCode.KEY_RESERVED, EventCode.KEY_RESERVED, EventCode.KEY_RESERVED, EventCode.KEY_RESERVED, EventCode.KEY_RESERVED, EventCode.KEY_INSERT, EventCode.KEY_DELETE, EventCode.KEY_RESERVED, EventCode.KEY_RESERVED, EventCode.KEY_LEFT,//0x4f
            /*0x50*/EventCode.KEY_HOME, EventCode.KEY_END, EventCode.KEY_MEDIA, EventCode.KEY_UP, EventCode.KEY_DOWN, EventCode.KEY_PAGEUP, EventCode.KEY_PAGEDOWN, EventCode.KEY_RESERVED,EventCode.KEY_RESERVED,EventCode.KEY_RIGHT, EventCode.KEY_NUMLOCK, EventCode.KEY_KP7, EventCode.KEY_KP4, EventCode.KEY_KP1, EventCode.KEY_RESERVED, EventCode.KEY_KPSLASH, //0x5f
            /*0x60*/EventCode.KEY_KP8, EventCode.KEY_KP5, EventCode.KEY_KP2, EventCode.KEY_KP0, EventCode.KEY_KPASTERISK, EventCode.KEY_KP9, EventCode.KEY_KP6, EventCode.KEY_KP3, EventCode.KEY_KPDOT, EventCode.KEY_KPMINUS, EventCode.KEY_KPPLUS, EventCode.KEY_RESERVED, EventCode.KEY_KPENTER, EventCode.KEY_RESERVED, EventCode.KEY_ESC, EventCode.KEY_RESERVED,//0x6f
            /*0x70*/EventCode.KEY_F1, EventCode.KEY_F2, EventCode.KEY_F3, EventCode.KEY_F4, EventCode.KEY_F5, EventCode.KEY_F6, EventCode.KEY_F7, EventCode.KEY_F8, EventCode.KEY_F9, EventCode.KEY_F10, EventCode.KEY_F11, EventCode.KEY_F12, EventCode.KEY_PRINT, EventCode.KEY_SCROLLLOCK, EventCode.KEY_PAUSE, EventCode.KEY_LEFTMETA,//0x7f
            /*0x80*/EventCode.KEY_RESERVED, EventCode.KEY_ROOT_MENU//0x81
    };

    @Override
    public EventCode[] getEventCodeMap() {
        return eventCodeMap;
    }
}