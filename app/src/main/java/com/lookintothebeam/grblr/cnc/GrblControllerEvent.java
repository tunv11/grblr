package com.lookintothebeam.grblr.cnc;


public class GrblControllerEvent {

    public static enum EventType {
        MACHINE_STATUS_UPDATED,
    }

    public EventType type;

    GrblControllerEvent(EventType e) {
        type = e;
    }
}