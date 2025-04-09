package com.abbytech.razer.analog.util;

public class Util {
    public static final int halfwayDown = Short.MAX_VALUE;//half-way
    private static final int quarterDown = 16383;
    private static final int calibratedValue = 8000;


    public static int mapToBool(int value) {
        return mapToBool(value, halfwayDown);
    }

    public static int mapToBoolQuarter(int value) {
        return mapToBool(value, quarterDown);
    }

    public static int mapToBoolCalibrated(int value) {
        return mapToBool(value, calibratedValue);
    }

    public static int mapToBool(int value, int keyDownThreshold) {
        value = value > keyDownThreshold ? 1 : 0;
        return value;
    }
}
