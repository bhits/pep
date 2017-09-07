package gov.samhsa.c2s.pep.service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
public class PepException extends RuntimeException {
    public static final String DEFAULT_MESSAGE = "PEP - Internal server error";

    public PepException() {
        super(DEFAULT_MESSAGE);
    }

    public PepException(String message) {
        super(message);
    }

    public PepException(String message, Throwable cause) {
        super(message, cause);
    }

    public PepException(Throwable cause) {
        super(cause);
    }

    public PepException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
