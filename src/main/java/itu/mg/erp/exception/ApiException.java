package itu.mg.erp.exception;

import lombok.Getter;

@Getter
public class ApiException extends RuntimeException {
    private final int errorCode;
    private final String message;
    private final String execType;

    public ApiException(int errorCode, String message, String execType) {
        super(message);
        this.errorCode = errorCode;
        this.message = message;
        this.execType = execType;
    }
}
