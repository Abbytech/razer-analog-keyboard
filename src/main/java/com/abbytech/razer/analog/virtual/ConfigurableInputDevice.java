package com.abbytech.razer.analog.virtual;

import com.abbytech.razer.analog.config.InputDeviceConfig;
import uk.co.bithatch.linuxio.EventCode;
import uk.co.bithatch.linuxio.InputDevice;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class ConfigurableInputDevice implements GenericDevice {
    private final InputDevice wrappedInputDevice;
    private final Map<EventCode, List<InputDeviceConfig.OutputMapping>> inputOutputMapping;
    private final List<EventCode> triggerAxes;
    private final int defaultActuationPoint;

    public ConfigurableInputDevice(InputDevice wrappedInputDevice, Map<EventCode,
                                           List<InputDeviceConfig.OutputMapping>> inputOutputMapping,
                                   int defaultActuationPoint) {
        this.wrappedInputDevice = wrappedInputDevice;
        this.inputOutputMapping = inputOutputMapping;
        this.defaultActuationPoint = defaultActuationPoint;
        Set<EventCode> outputCapabilities = inputOutputMapping
                .values()
                .stream()
                .flatMap(Collection::stream)
                .map(InputDeviceConfig.OutputMapping::getEventCode)
                .collect(Collectors.toSet());

        wrappedInputDevice.getCapabilities().addAll(outputCapabilities);

        triggerAxes = inputOutputMapping.values().stream().flatMap(Collection::stream)
                .collect(Collectors.groupingBy(InputDeviceConfig.OutputMapping::getEventCode))
                .values()
                .stream()
                .filter(outputMappings ->
                        outputMappings.stream().allMatch(InputDeviceConfig.OutputMapping::isPositive)
                                ||
                                outputMappings.stream().noneMatch(InputDeviceConfig.OutputMapping::isPositive))
                .map(List::getFirst)
                .map(InputDeviceConfig.OutputMapping::getEventCode)
                .toList();
    }

    @Override
    public boolean isOpen() {
        return wrappedInputDevice.isOpen();
    }

    @Override
    public void close() throws IOException {
        wrappedInputDevice.close();
    }

    @Override
    public void handle(List<InputDevice.Event> events) {
        List<InputDevice.Event> mappedEvents = mapToInputDeviceEvents(events);
        mappedEvents.forEach(inputDeviceEvent -> {
            try {
                wrappedInputDevice.emit(inputDeviceEvent, false);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        if (!mappedEvents.isEmpty()) {
            try {
                wrappedInputDevice.syn();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private List<InputDevice.Event> mapToInputDeviceEvents(List<InputDevice.Event> eventParts) {
        return eventParts.stream().map(this::mapToInputEventGeneric).flatMap(Collection::stream)
                .collect(Collectors.groupingBy(InputDevice.Event::getCode))
                .values()
                .stream()
                .map(events -> {
                    InputDevice.Event event = events.stream().reduce(new InputDevice.Event(null, 0), (event1, event2) -> {
                        int event1Value = event1.getValue();
                        int event2Value = event2.getValue();
                        int sum = event1Value + event2Value;

                        return new InputDevice.Event(event1.getCode() != null ? event1.getCode() : event2.getCode(), sum);
                    });
                    return new InputDevice.Event(event.getCode(), Math.clamp(event.getValue(), Short.MIN_VALUE, Short.MAX_VALUE));
                }).collect(Collectors.toList());
    }

    private List<InputDevice.Event> mapToInputEventGeneric(InputDevice.Event event) {
        int inputValue = event.getValue();
        int axisValue = inputValue / 2; //restrict to Short.MAX_VALUE; output values higher/lower than this seem to cause axes to go out of range
        EventCode inputEventCode = event.getCode();

        List<InputDeviceConfig.OutputMapping> outputMappings = inputOutputMapping.get(inputEventCode);

        return outputMappings.stream().map(outputMapping -> {
            EventCode outputEventCode = outputMapping.getEventCode();
            int outputValue;
            if (outputEventCode.isButton()) {
                int actuationPoint = outputMapping.getActuationPoint() != 0 ? outputMapping.getActuationPoint() : defaultActuationPoint;
                outputValue = inputValue > actuationPoint ? 1 : 0;
                return new InputDevice.Event(outputEventCode, outputValue);
            } else if (!outputEventCode.isKey())/*implied isAxis*/ {
                boolean positive = outputMapping.isPositive();
                if (triggerAxes.contains(outputEventCode)) {
                    int triggerAxisValue = inputValue - Short.MAX_VALUE;
                    outputValue = positive ? triggerAxisValue : triggerAxisValue * -1;
                } else {
                    outputValue = positive ? axisValue : axisValue * -1;
                }
            } else {
                throw new IllegalArgumentException("mapping to key is unsupported");
            }
            return new InputDevice.Event(outputEventCode, outputValue);
        }).collect(Collectors.toList());
    }

    @Override
    public boolean canHandle(EventCode eventCode) {
        return inputOutputMapping.containsKey(eventCode);
    }

    @Override
    public void open() throws IOException {
        wrappedInputDevice.open();
    }
}
