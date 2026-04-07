package ecommerce.exceptionHandling;

import ecommerce.dto.GenericResponseDto;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.jetbrains.annotations.NotNull;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.DisabledException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLIntegrityConstraintViolationException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<GenericResponseDto<Object>> handleBadRequest(BadRequestException ex) {
        return ResponseEntity.badRequest().body(
                GenericResponseDto.error("Bad Request", ex.getMessage(), HttpStatus.BAD_REQUEST.value())
        );
    }

    // 401 - Unauthorized
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<GenericResponseDto<Object>> handleUnauthorized(UnauthorizedException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                GenericResponseDto.error("Unauthorized", ex.getMessage(), HttpStatus.UNAUTHORIZED.value())
        );
    }

    // 403 - Forbidden
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<GenericResponseDto<Object>> handleForbidden(ForbiddenException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                GenericResponseDto.error("Forbidden", ex.getMessage(), HttpStatus.FORBIDDEN.value())
        );
    }

    // 503 - Service Unavailable
    @ExceptionHandler(ServiceUnavailableException.class)
    public ResponseEntity<GenericResponseDto<Object>> handleServiceUnavailable(ServiceUnavailableException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(
                GenericResponseDto.error("Service Unavailable", ex.getMessage(), HttpStatus.SERVICE_UNAVAILABLE.value())
        );
    }

    // 404 - Resource Not Found
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<GenericResponseDto<Object>> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                GenericResponseDto.error("Not Found", ex.getMessage(), HttpStatus.NOT_FOUND.value())
        );
    }

    // 400 - Validation errors from @Valid (DTO fields)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<GenericResponseDto<Object>> handleValidation(MethodArgumentNotValidException ex) {
        BindingResult result = ex.getBindingResult();
        String firstErrorMessage = result.getFieldErrors()
                .stream()
                .findFirst()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .orElse("Validation error");

        return ResponseEntity.badRequest().body(
                GenericResponseDto.error("Bad Request", firstErrorMessage, HttpStatus.BAD_REQUEST.value())
        );
    }

    // 400 - Constraint violation (method-level validations)
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<GenericResponseDto<Object>> handleConstraintViolation(ConstraintViolationException ex) {
        String firstErrorMessage = ex.getConstraintViolations()
                .stream()
                .findFirst()
                .map(err -> err.getPropertyPath() + ": " + err.getMessage())
                .orElse("Validation error");

        return ResponseEntity.badRequest().body(
                GenericResponseDto.error("Bad Request", firstErrorMessage, HttpStatus.BAD_REQUEST.value())
        );
    }

    // 500 - Internal Server Error (catch-all)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<GenericResponseDto<Object>> handleGeneralException(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                GenericResponseDto.error("Internal Server Error", ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value())
        );
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<Map<String, Object>> handleDisabledException(DisabledException ex) {

        Map<String, Object> body = new LinkedHashMap<>();

        // Formatting the date to match your JSON structure
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        body.put("timestamp", LocalDateTime.now().format(formatter));
        body.put("status", HttpStatus.FORBIDDEN.value()); // 403 status code
        body.put("error", "Forbidden");
        body.put("message", "Account is not verified. Please complete OTP verification.");
        body.put("data", null);

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }
}
