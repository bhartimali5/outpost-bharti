package com.thub.outpost.common.error;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * One place that turns every exception thrown from a controller into an RFC 7807
 * {@link ProblemDetail} response (content type application/problem+json):
 * {type, title, status, detail, instance, code, errors?}.
 *
 * {@code @RestControllerAdvice} makes these handler methods apply to all controllers.
 * The base class already maps Spring MVC's own exceptions (405, 415, unknown URL, ...) to
 * ProblemDetail; we override a few of them to add our {@code code} and {@code errors[]}.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Our own business exceptions: status and code come from the exception itself. */
    @ExceptionHandler(ApiException.class)
    ResponseEntity<Object> handleApiException(ApiException ex, WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(ex.getStatus(), ex.getMessage());
        problem.setTitle(ex.getStatus().getReasonPhrase());
        problem.setProperty("code", ex.getCode());
        return handleExceptionInternal(ex, problem, new HttpHeaders(), ex.getStatus(), request);
    }

    /** @Valid failed on a request body: 400 with one entry per invalid field. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> Map.of("field", fe.getField(),
                        "message", fe.getDefaultMessage() == null ? "invalid value" : fe.getDefaultMessage()))
                .toList();
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed");
        problem.setTitle("Bad Request");
        problem.setProperty("code", "VALIDATION_FAILED");
        problem.setProperty("errors", errors);
        return handleExceptionInternal(ex, problem, headers, HttpStatus.BAD_REQUEST, request);
    }

    /** Malformed JSON, wrong types, unknown enum value, etc. */
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        // Deliberately generic: the raw parser message can reveal internal class names.
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Request body is missing or malformed");
        problem.setTitle("Bad Request");
        problem.setProperty("code", "MALFORMED_REQUEST");
        return handleExceptionInternal(ex, problem, headers, HttpStatus.BAD_REQUEST, request);
    }

    /** Authenticated but not allowed (for example @PreAuthorize failed). */
    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<Object> handleAccessDenied(AccessDeniedException ex, WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN,
                "You do not have permission to do this");
        problem.setTitle("Forbidden");
        problem.setProperty("code", "ACCESS_DENIED");
        return handleExceptionInternal(ex, problem, new HttpHeaders(), HttpStatus.FORBIDDEN, request);
    }

    /** Not authenticated, or bad credentials. The message stays generic on purpose. */
    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<Object> handleAuthentication(AuthenticationException ex, WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED,
                "Authentication is required or the credentials are invalid");
        problem.setTitle("Unauthorized");
        problem.setProperty("code", "UNAUTHENTICATED");
        return handleExceptionInternal(ex, problem, new HttpHeaders(), HttpStatus.UNAUTHORIZED, request);
    }

    /** Safety net for anything unexpected: 500 with no stack trace or internal message in the body. */
    @ExceptionHandler(Exception.class)
    ResponseEntity<Object> handleUnexpected(Exception ex, WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred");
        problem.setTitle("Internal Server Error");
        problem.setProperty("code", "INTERNAL_ERROR");
        return handleExceptionInternal(ex, problem, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    /**
     * Every response built by this class (including the ones inherited from the base class)
     * passes through here, so this is where we guarantee {@code instance} and {@code code}.
     */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
            HttpStatusCode statusCode, WebRequest request) {
        logException(ex, statusCode, request);
        ResponseEntity<Object> response = super.handleExceptionInternal(ex, body, headers, statusCode, request);
        if (response != null && response.getBody() instanceof ProblemDetail problem) {
            if (problem.getInstance() == null && request instanceof ServletWebRequest servletRequest) {
                HttpServletRequest httpRequest = servletRequest.getRequest();
                problem.setInstance(URI.create(httpRequest.getRequestURI()));
            }
            if (problem.getProperties() == null || !problem.getProperties().containsKey("code")) {
                // Framework errors without a custom code (404 unknown URL, 405, 415, ...)
                problem.setProperty("code", HttpStatus.valueOf(statusCode.value()).name());
            }
        }
        return response;
    }

    /**
     * 5xx means our bug: ERROR with the full stack trace so we can debug it.
     * 4xx means the client's mistake: WARN with a one-line summary and no stack trace (it would only be noise).
     */
    private void logException(Exception ex, HttpStatusCode statusCode, WebRequest request) {
        String path = request instanceof ServletWebRequest servletRequest
                ? servletRequest.getRequest().getRequestURI() : "?";
        if (statusCode.is5xxServerError()) {
            log.error("Unhandled exception on {} -> {}", path, statusCode.value(), ex);
        } else {
            log.warn("Request rejected on {} -> {}: {}", path, statusCode.value(), ex.getMessage());
        }
    }
}
