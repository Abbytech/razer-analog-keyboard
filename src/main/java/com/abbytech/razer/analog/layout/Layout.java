package com.abbytech.razer.analog.layout;

import uk.co.bithatch.linuxio.EventCode;

import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface Layout {
    EventCode[] getEventCodeMap();
    default Collection<EventCode> getCapabilities(){
        return Stream.of(getEventCodeMap()).filter(eventCode -> eventCode != EventCode.KEY_RESERVED).collect(Collectors.toList());
    }
}
