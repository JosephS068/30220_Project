package channel;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.UNAUTHORIZED, reason = "User must be admin account to access this feature")
public class RequiresAdminException extends RuntimeException {
}
