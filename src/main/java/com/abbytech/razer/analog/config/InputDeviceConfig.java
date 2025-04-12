package com.abbytech.razer.analog.config;

import lombok.Data;
import uk.co.bithatch.linuxio.EventCode;

import java.util.List;
import java.util.Map;

@Data
public class InputDeviceConfig {
    private String deviceName;
    private short vendorId;
    private short productId;
    private Map<EventCode, List<OutputMapping>> inputOutputMapping;
    private int defaultActuationPoint; //value between 0-65535; used to determine when a button is considered to be pressed(down).

    @Data
    public static class OutputMapping {
        private EventCode eventCode;
        private boolean positive;//used to determine button axis direction (e.g. left or right) in-case event code is an axis
        private int actuationPoint; //value between 0-65535; used to determine when a button is considered to be pressed(down)
    }
}
