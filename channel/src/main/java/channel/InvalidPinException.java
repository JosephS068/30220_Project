package channel;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.UNAUTHORIZED, reason = "Pin provided was incorrect")
public class InvalidPinException extends RuntimeException {
}
