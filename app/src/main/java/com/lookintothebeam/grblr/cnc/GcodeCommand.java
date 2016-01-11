package com.lookintothebeam.grblr.cnc;

public class GcodeCommand {

    public static enum Status {
        QUEUED, RUNNING, COMPLETE
    };

    private String command;
    private Status status;

    public GcodeCommand(String command) {
        this.command = command;
        this.status = Status.QUEUED;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getCommand() {
        return this.command;
    }

    public Status getStatus() {
        return this.status;
    }
}
