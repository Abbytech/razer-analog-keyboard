package com.abbytech;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.usb4java.*;


import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.abbytech.Constants.*;

public class USB {

    private static final int HID_DATA_LENGTH = 48;

    private static final Map<String, String> keyCodeMap = new HashMap<>();
    private static DeviceHandle deviceHandle;

    static {
        keyCodeMap.put("12", "W");
        keyCodeMap.put("1f", "A");
        keyCodeMap.put("20", "S");
        keyCodeMap.put("21", "D");
    }

    static class Event {
        private final String key;
        private final Integer value;

        public Event(String key, Integer value) {
            this.key = key;
            this.value = value;
        }

        public String getKey() {
            return key;
        }

        public Integer getValue() {
            return value;
        }

        @Override
        public String toString() {
            return "Event{" +
                    "key='" + key + '\'' +
                    ", value=" + value +
                    '}';
        }
    }

    interface EventListener {
        void onEvent(Event event);
    }

    public static void listen(EventListener listener) throws DecoderException {
        init();
        Device device = findDevice(VENDOR_RAZER, HUNTSMAN_V3_PRO).get(0);
        deviceHandle = claimDevice(device);

        sendCommand(deviceHandle, Constants.setDriverDeviceMode);
        System.out.println("waiting for input");
        while (true) {
            Event event = readEvent();
            listener.onEvent(event);
            System.out.println(event);
        }
    }

    private static Event readEvent() {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(HID_DATA_LENGTH);
        IntBuffer transferred = IntBuffer.allocate(1);
        int result = LibUsb.interruptTransfer(deviceHandle, ENDPOINT_IN_ANALOG, byteBuffer, transferred, 0);
        char[] hidData = Hex.encodeHex(byteBuffer);
        String keyCode = new String(new char[]{hidData[2], hidData[3]});
        String valueString = new String(new char[]{hidData[4], hidData[5], hidData[6], hidData[7]});
        String key = Optional.ofNullable(keyCodeMap.get(keyCode)).orElse(String.format("UNMAPPED:%s,hidData:%s", keyCode, new String(hidData)));
        Integer value = Integer.decode("0x" + valueString);
        checkResult(result, false);
        return new Event(key, value);
    }

    public static void reset() throws DecoderException {
        sendCommand(deviceHandle, Constants.setNormalDeviceMode);
        releaseDevice(deviceHandle);
    }

    private static DeviceHandle claimDevice(Device device) {
        deviceHandle = openDevice(device);
        claimInterface(Constants.interfaceNumber);
        claimInterface(1);
        claimInterface(3);
        return deviceHandle;
    }

    private static void claimInterface(int interfaceNumber) {
        detachDriver(interfaceNumber);
        int result = LibUsb.claimInterface(deviceHandle, interfaceNumber);
        checkResult(result, true);
    }

    private static void releaseDevice(DeviceHandle handle) {
        releaseInterface(Constants.interfaceNumber);
        releaseInterface(1);
        releaseInterface(3);
        LibUsb.close(handle);
    }

    private static void releaseInterface(int interfaceNumber) {
        int result = LibUsb.releaseInterface(deviceHandle, interfaceNumber);
        checkResult(result, false);
        attachDriver(interfaceNumber);
    }

    private static void checkResult(int result, boolean shouldThrow) {
        if (result != LibUsb.SUCCESS) {
            if (shouldThrow) {
                throw new LibUsbException(result);
            } else {
                String error = LibUsb.strError(result);
                Exception exception = new Exception(error);
                exception.printStackTrace();
            }
        }
    }

    public static List<Device> findDevice(short vendorId, short productId) {
        // Read the USB device list
        DeviceList list = new DeviceList();
        int result = LibUsb.getDeviceList(null, list);
        if (result < 0) throw new LibUsbException("Unable to get device list", result);
        List<Device> matchingDevices = StreamSupport.stream(list.spliterator(), false)
                .filter(device -> {
                    DeviceDescriptor descriptor = new DeviceDescriptor();
                    int deviceDescriptorResult = LibUsb.getDeviceDescriptor(device, descriptor);
                    if (deviceDescriptorResult != LibUsb.SUCCESS)
                        throw new LibUsbException("Unable to read device descriptor", deviceDescriptorResult);
                    return (descriptor.idVendor() == vendorId && descriptor.idProduct() == productId);
                }).collect(Collectors.toList());

        LibUsb.freeDeviceList(list, true);
        if (matchingDevices.size() > 1) {
            System.out.println("how?");
        }
        return matchingDevices;
    }

    public static void init() {
        int result = LibUsb.init(null);
        if (result != LibUsb.SUCCESS) throw new LibUsbException("Unable to initialize libusb.", result);
    }

    public static void sendCommand(DeviceHandle handle, String mode) throws DecoderException {
        byte[] setDeviceMode = Hex.decodeHex(mode);
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(setDeviceMode.length);
        byteBuffer.put(setDeviceMode);
        int result = LibUsb.controlTransfer(handle, (byte) 0x21, (byte) 0x09, (short) 0x0300, (short) 0x0003, byteBuffer, 0);
        checkResult(result, false);
    }

    private static DeviceHandle openDevice(Device device) {
        DeviceHandle handle = new DeviceHandle();
        int devResult = LibUsb.open(device, handle);
        if (devResult != LibUsb.SUCCESS) throw new LibUsbException("Unable to open USB device", devResult);
        return handle;
    }

    public static void detachDriver(int interfaceNumber) {
        int result = LibUsb.detachKernelDriver(deviceHandle, interfaceNumber);
        if (result != LibUsb.SUCCESS && result != LibUsb.ERROR_NOT_FOUND)
            throw new LibUsbException("Unable to detach kernel driver", result);
    }

    private static void attachDriver(int interfaceNumber) {
        int result = LibUsb.attachKernelDriver(deviceHandle, interfaceNumber);
        if (result != LibUsb.SUCCESS && result != LibUsb.ERROR_BUSY)
            throw new LibUsbException("Unable to re-attach kernel driver", result);
    }
}
