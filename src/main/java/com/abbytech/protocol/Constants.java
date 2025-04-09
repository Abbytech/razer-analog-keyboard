package com.abbytech.protocol;

import org.usb4java.LibUsb;

public class Constants {
    public static final short VENDOR_RAZER = 0x1532;
    public static final short HUNTSMAN_V3_PRO = 0x02a6;
    static final int interfaceNumber = 0;

    public static final byte COMMAND_LENGTH = 90;
    public static final String setDriverDeviceMode = "00ff00000002000403000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000500";
    public static final String setNormalDeviceMode = "00ff00000002000400000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000600";

    public static final byte ENDPOINT_IN_DIGITAL = LibUsb.ENDPOINT_IN | 0x01;
    public static final byte ENDPOINT_IN_ANALOG = LibUsb.ENDPOINT_IN | 0x02;
    static final int HID_DATA_LENGTH_DIGITAL = 8;
    static final int HID_DATA_LENGTH = 48;

    public static int MAX_X_AXIS_VALUE = 65535;
    public static int MIN_X_AXIS_VALUE = 0;

    public static int MAX_RZ_AXIS_VALUE = 65535;
    public static int MIN_RZ_AXIS_VALUE = 0;
}
