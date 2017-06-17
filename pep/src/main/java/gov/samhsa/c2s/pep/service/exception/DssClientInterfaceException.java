package gov.samhsa.c2s.pep.service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
public class DssClientInterfaceException extends RuntimeException {
    public static final String DEFAULT_MESSAGE = "Document not found";

    public DssClientInterfaceException() {
        super(DEFAULT_MESSAGE);
    }

    public DssClientInterfaceException(String message) {
        super(message);
    }

    public DssClientInterfaceException(String message, Throwable cause) {
        super(message, cause);
    }

    public DssClientInterfaceException(Throwable cause) {
        super(cause);
    }

    public DssClientInterfaceException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
