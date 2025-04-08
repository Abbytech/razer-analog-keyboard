package com.abbytech.protocol;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.usb4java.*;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.abbytech.protocol.Constants.*;

public class USB {
    private static DeviceHandle deviceHandle;
    private static final ByteBuffer commandByteBuffer = ByteBuffer.allocateDirect(COMMAND_LENGTH);
    private static final ByteBuffer hidDataBuffer = ByteBuffer.allocateDirect(HID_DATA_LENGTH);
    private static final IntBuffer hidInterruptResult = IntBuffer.allocate(1);


    public static void openDevice() throws DecoderException {
        Runtime.getRuntime().addShutdownHook(new Thread(USB::shutdown));
        init();
        Device device = findDevice(VENDOR_RAZER, HUNTSMAN_V3_PRO).get(0);
        deviceHandle = claimDevice(device);
        sendCommand(deviceHandle, Constants.setDriverDeviceMode);
        System.out.println("device opened");
    }

    private static void init() {
        int result = LibUsb.init(null);
        if (result != LibUsb.SUCCESS) throw new LibUsbException("Unable to initialize libusb.", result);
    }

    public static void closeDevice() throws DecoderException {
        sendCommand(deviceHandle, Constants.setNormalDeviceMode);
        releaseDevice(deviceHandle);
    }

    public static ByteBuffer readHIDData() {
        hidDataBuffer.clear();
        hidInterruptResult.clear();
        int result = LibUsb.interruptTransfer(deviceHandle, ENDPOINT_IN_ANALOG, hidDataBuffer, hidInterruptResult, 0);
        checkResult(result, false);
        return hidDataBuffer;
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

    private static List<Device> findDevice(short vendorId, short productId) {
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

    public static void sendCommand(DeviceHandle handle, String mode) throws DecoderException {
        commandByteBuffer.clear();
        byte[] setDeviceMode = Hex.decodeHex(mode);
        commandByteBuffer.put(setDeviceMode);
        int result = LibUsb.controlTransfer(handle, (byte) 0x21, (byte) 0x09, (short) 0x0300, (short) 0x0003, commandByteBuffer, 0);
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

    private static void shutdown() {
        try {
            USB.closeDevice();
        } catch (Exception e) {
            //ignored
            e.printStackTrace();
        }
    }
}