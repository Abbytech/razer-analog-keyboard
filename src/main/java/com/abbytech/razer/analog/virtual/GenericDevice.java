package com.abbytech.razer.analog.virtual;

import uk.co.bithatch.linuxio.EventCode;
import uk.co.bithatch.linuxio.InputDevice;

import java.io.IOException;
import java.util.List;

public interface GenericDevice {
    boolean isOpen();
    void close() throws IOException;
    void handle(List<InputDevice.Event> events);
    boolean canHandle(EventCode eventCode);
    void open() throws IOException;
}
