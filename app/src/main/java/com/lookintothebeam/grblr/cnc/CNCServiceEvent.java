package com.lookintothebeam.grblr.cnc;


public class CNCServiceEvent {

    public static enum EventType {
        USB_DEVICE_CONNECTED,
        USB_DEVICE_DISCONNECTED,
        FILE_OPEN_SUCCESS,
        FILE_OPEN_FAILURE,
        FILE_COMMAND_STATUS_UPDATED,
    }

    public EventType type;

    CNCServiceEvent(EventType e) {
        type = e;
    }
}
