package rw.ac.rca.isoko.common.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import rw.ac.rca.isoko.common.exception.ApiException;

import java.net.URI;
import java.util.stream.Collectors;

@RestControllerAdvice
public class RestExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(RestExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    ProblemDetail handleApi(ApiException ex, HttpServletRequest request) {
        log.warn("api_error code={} status={} path={} message={}",
                ex.code(), ex.status().value(), request.getRequestURI(), ex.getMessage());
        return problem(ex.status(), ex.code(), ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + " " + err.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return problem(HttpStatus.BAD_REQUEST, "validation-error", detail, request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ProblemDetail handleConstraint(ConstraintViolationException ex, HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, "validation-error", ex.getMessage(), request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail handleUnreadable(HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, "malformed-json", "Request body is not valid JSON.", request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    ProblemDetail handleDenied(HttpServletRequest request) {
        return problem(HttpStatus.FORBIDDEN, "forbidden", "You are not allowed to perform this action.", request);
    }

    @ExceptionHandler(AuthenticationException.class)
    ProblemDetail handleAuth(HttpServletRequest request) {
        return problem(HttpStatus.UNAUTHORIZED, "unauthorized", "Authentication is required.", request);
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnknown(Exception ex, HttpServletRequest request) {
        log.error("unhandled_error path={}", request.getRequestURI(), ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "internal-error",
                "Something went wrong. Quote this trace id if you contact support.", request);
    }

    @ExceptionHandler({org.springframework.dao.DataIntegrityViolationException.class,
            org.springframework.orm.ObjectOptimisticLockingFailureException.class})
    ProblemDetail handleDataConflict(HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT,"conflict","This change conflicts with current data. Refresh and try again.",request);
    }

    @ExceptionHandler({org.springframework.web.bind.MissingRequestHeaderException.class,
            org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class})
    ProblemDetail handleInput(HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST,"validation-error","A required header or parameter is missing or invalid.",request);
    }

    private ProblemDetail problem(HttpStatus status, String code, String detail, HttpServletRequest request) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setTitle(status.getReasonPhrase());
        pd.setType(URI.create("https://isoko.rw/errors/" + code));
        pd.setInstance(URI.create(request.getRequestURI()));
        pd.setProperty("code", code);
        pd.setProperty("traceId", MDC.get(TraceIdFilter.MDC_KEY));
        return pd;
    }
}
