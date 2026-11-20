package com.projectii.Nei.exception;

public class ActiveWeekException extends RuntimeException {
    public ActiveWeekException(String noActiveWeekFound) {
        super(noActiveWeekFound);
    }
}
