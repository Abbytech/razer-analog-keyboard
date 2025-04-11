package com.abbytech.razer.analog.protocol;

import org.usb4java.*;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.abbytech.razer.analog.protocol.Constants.*;

public class USB {
    private final Thread hook = new Thread(this::shutdown);
    private DeviceHandle deviceHandle;
    private final ByteBuffer commandByteBuffer = ByteBuffer.allocateDirect(COMMAND_LENGTH);
    private final ByteBuffer hidDataBuffer = ByteBuffer.allocateDirect(HID_DATA_LENGTH);
    private final IntBuffer hidInterruptResult = IntBuffer.allocate(1);
    private boolean deviceOpen = false;

    static {
        init();
    }

    private static void init() {
        int result = LibUsb.init(null);
        if (result != LibUsb.SUCCESS) throw new LibUsbException("Unable to initialize libusb.", result);
    }

    public void openDevice() throws LibUsbException {
        if (deviceOpen) {
            throw new IllegalStateException("device already open");
        }
        Runtime.getRuntime().addShutdownHook(hook);
        Device device = findDevice(VENDOR_RAZER, HUNTSMAN_V3_PRO).get(0);
        deviceHandle = claimDevice(device);
        sendCommand(deviceHandle, setDriveDeviceMode);
        deviceOpen = true;
        System.out.println("device opened");
    }

    public void closeDevice() throws LibUsbException {
        if (!deviceOpen) {
            throw new IllegalStateException("device not open");
        }
        if (!Thread.currentThread().equals(hook)) {
            Runtime.getRuntime().removeShutdownHook(hook);
        }
        System.out.println("closing device");
        sendCommand(deviceHandle, setNormalDeviceMode);
        releaseDevice(deviceHandle);
        deviceOpen = false;
    }

    public ByteBuffer readHIDData() {
        if (!deviceOpen) {
            throw new IllegalStateException("attempt to read closed device");
        }
        hidDataBuffer.clear();
        hidInterruptResult.clear();
        int result = LibUsb.interruptTransfer(deviceHandle, ENDPOINT_IN_ANALOG, hidDataBuffer, hidInterruptResult, 0);
        checkResult(result, false);
        return hidDataBuffer;
    }


    private DeviceHandle claimDevice(Device device) {
        deviceHandle = openDevice(device);
        claimInterface(Constants.interfaceNumber);
        claimInterface(1);
        claimInterface(3);
        return deviceHandle;
    }

    private void claimInterface(int interfaceNumber) {
        detachDriver(interfaceNumber);
        int result = LibUsb.claimInterface(deviceHandle, interfaceNumber);
        checkResult(result, true);
    }

    private void releaseDevice(DeviceHandle handle) {
        releaseInterface(Constants.interfaceNumber);
        releaseInterface(1);
        releaseInterface(3);
        LibUsb.close(handle);
    }

    private void releaseInterface(int interfaceNumber) {
        int result = LibUsb.releaseInterface(deviceHandle, interfaceNumber);
        checkResult(result, false);
        attachDriver(interfaceNumber);
    }

    private void checkResult(int result, boolean shouldThrow) {
        if (result != LibUsb.SUCCESS && result != COMMAND_LENGTH) {
            if (shouldThrow) {
                throw new LibUsbException(result);
            } else {
                String error = LibUsb.strError(result);
                Exception exception = new Exception(error);
                System.err.printf("error code: %d", result);
                exception.printStackTrace();
            }
        }
    }

    private List<Device> findDevice(short vendorId, short productId) {
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

    public void sendCommand(DeviceHandle handle, byte[] mode) {
        commandByteBuffer.clear();
        commandByteBuffer.put(mode);
        int result = LibUsb.controlTransfer(handle, (byte) 0x21, (byte) 0x09, (short) 0x0300, (short) 0x0003, commandByteBuffer, 0);
        checkResult(result, false);
    }

    private DeviceHandle openDevice(Device device) {
        DeviceHandle handle = new DeviceHandle();
        int devResult = LibUsb.open(device, handle);
        if (devResult != LibUsb.SUCCESS) throw new LibUsbException("Unable to open USB device", devResult);
        return handle;
    }

    public void detachDriver(int interfaceNumber) {
        int result = LibUsb.detachKernelDriver(deviceHandle, interfaceNumber);
        if (result != LibUsb.SUCCESS && result != LibUsb.ERROR_NOT_FOUND)
            throw new LibUsbException("Unable to detach kernel driver", result);
    }

    private void attachDriver(int interfaceNumber) {
        int result = LibUsb.attachKernelDriver(deviceHandle, interfaceNumber);
        if (result != LibUsb.SUCCESS && result != LibUsb.ERROR_BUSY && result != LibUsb.ERROR_NOT_FOUND)
            throw new LibUsbException("Unable to re-attach kernel driver", result);
    }

    private void shutdown() {
        try {
            closeDevice();
        } catch (Exception e) {
            //ignored
            e.printStackTrace();
        }
    }
}