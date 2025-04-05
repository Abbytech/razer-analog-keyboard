package com.abbytech;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.usb4java.*;


import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.abbytech.Constants.*;

public class USB {

    private static final int HID_DATA_LENGTH = 48;

    private static final Map<String, Key> keyCodeMap = new HashMap<>();
    private static final int initialCapacity = 15;
    private static DeviceHandle deviceHandle;


    enum Key {
        W, A, S, D, FN, HOME, UNMAPPED,NO_KEY
    }

    static {
        keyCodeMap.put("12", Key.W);
        keyCodeMap.put("1f", Key.A);
        keyCodeMap.put("20", Key.S);
        keyCodeMap.put("21", Key.D);
        keyCodeMap.put("3b", Key.FN);
        keyCodeMap.put("50", Key.HOME);
        keyCodeMap.put("00",Key.NO_KEY);
    }

    static class EventKey {
        private final Key key;
        private final Integer value;

        public EventKey(Key key, Integer value) {
            this.key = key;
            this.value = value;
        }

        public Key getKey() {
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

    static class Event {
        private final List<EventKey> eventKeys;

        Event(List<EventKey> eventKeys) {
            this.eventKeys = eventKeys;
        }

        public List<EventKey> getEventParts() {
            return eventKeys;
        }

        @Override
        public String toString() {
            return "Event{" +
                    "eventKeys=" + eventKeys +
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
            List<EventKey> eventParts = event.getEventParts();
            if (eventParts.size() == 2) {
                USB.EventKey eventKey1 = eventParts.get(0);
                USB.EventKey eventKey2 = eventParts.get(1);
                USB.Key key1 = eventKey1.getKey();
                USB.Key key2 = eventKey2.getKey();
                Integer value1 = eventKey1.getValue();
                Integer value2 = eventKey2.getValue();

                if (key1.equals(USB.Key.FN) && key2.equals(USB.Key.HOME)) {
                    if (value1 > Short.MAX_VALUE && value2 > Short.MAX_VALUE) {
                        System.out.println("terminating");
                        break;
                    }
                }
            }else{
                listener.onEvent(event);
                System.out.println(event);
            }
        }
    }

    private static Event readEvent() {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(HID_DATA_LENGTH);
        IntBuffer transferred = IntBuffer.allocate(1);
        int result = LibUsb.interruptTransfer(deviceHandle, ENDPOINT_IN_ANALOG, byteBuffer, transferred, 0);
        String hidData = new String(Hex.encodeHex(byteBuffer));
        List<EventKey> eventKeys = decode(hidData).stream().map(hidPart -> {
            String keyCode = hidPart.getKeyCode();
            Integer value = hidPart.getValue();
            Key key = Optional.ofNullable(keyCodeMap.get(keyCode)).orElse(Key.UNMAPPED);
            return new EventKey(key, value);
        }).filter(eventKey -> !(eventKey.getKey().equals(Key.NO_KEY) && eventKey.getValue()==0))
                .collect(Collectors.toList());

        checkResult(result, false);
        return new Event(eventKeys);
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
