package channel;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.UNAUTHORIZED, reason = "User is not authenticated from this server")
public class NotAuthorizedException extends RuntimeException {
}
