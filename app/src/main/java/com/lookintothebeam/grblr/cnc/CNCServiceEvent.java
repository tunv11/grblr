package com.lookintothebeam.grblr.cnc;


public class CNCServiceEvent {

    public static enum EventType {
        USB_DEVICE_CONNECTED, USB_DEVICE_DISCONNECTED,
    }

    public EventType type;

    CNCServiceEvent(EventType e) {
        type = e;
    }
}
