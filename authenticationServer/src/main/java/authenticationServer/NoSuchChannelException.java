package authenticationServer;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.NOT_FOUND, reason = "Channel called does not exist")
public class NoSuchChannelException extends RuntimeException {
}
