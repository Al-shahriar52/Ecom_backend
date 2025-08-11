package ecommerce.exceptionHandling;

import ecommerce.dto.GenericResponseDto;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.jetbrains.annotations.NotNull;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
//    private static final DateTimeFormatter FORMATTER =
//            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
//
//    @ExceptionHandler(ResourceNotFound.class)
//    protected ResponseEntity<ErrorMessage> handleResourceNotFound(@NotNull ResourceNotFound ex) {
//
//        ErrorMessage message = new ErrorMessage(ex.getMessage());
//        return new ResponseEntity<>(message, HttpStatus.NOT_FOUND) ;
//    }
//
//    @ExceptionHandler(MethodArgumentNotValidException.class)
//    @ResponseStatus(HttpStatus.BAD_REQUEST)
//    @ResponseBody
//    protected ResponseEntity<String> handleValidationException(MethodArgumentNotValidException ex) {
//        BindingResult bindingResult = ex.getBindingResult();
//        StringBuilder errorMessage = new StringBuilder();
//
//        for (FieldError fieldError : bindingResult.getFieldErrors()) {
//            errorMessage.append(fieldError.getField())
//                    .append(": ")
//                    .append(fieldError.getDefaultMessage())
//                    .append(". ")
//                    .append("\n");
//        }
//        return ResponseEntity.badRequest().body(errorMessage.toString());
//    }
//
//    @ExceptionHandler(ConstraintViolationException.class)
//    @ResponseStatus(HttpStatus.BAD_REQUEST)
//    @ResponseBody
//    protected ResponseEntity<String> handleConstraintViolationException(ConstraintViolationException ex) {
//        StringBuilder errorMessage = new StringBuilder();
//
//        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
//            errorMessage.append(violation.getPropertyPath())
//                    .append(": ")
//                    .append(violation.getMessage())
//                    .append(". ")
//                    .append("\n");
//        }
//        return ResponseEntity.badRequest().body(errorMessage.toString());
//    }
//
//    @ExceptionHandler({SQLIntegrityConstraintViolationException.class, DataIntegrityViolationException.class})
//    protected ResponseEntity<ErrorMessage> handleSqlIntegrityViolation(SQLIntegrityConstraintViolationException ex) {
//
//        ErrorMessage message = new ErrorMessage(ex.getMessage());
//        return new ResponseEntity<>(message, HttpStatus.CONFLICT);
//    }
//
//    @ExceptionHandler(BadRequestException.class)
//    public ResponseEntity<Map<String, Object>> handleBadRequest(BadRequestException ex) {
//        Map<String, Object> body = new HashMap<>();
//        body.put("timestamp", LocalDateTime.now().format(FORMATTER));
//        body.put("status", HttpStatus.BAD_REQUEST.value());
//        body.put("error", "Bad Request");
//        body.put("message", ex.getMessage());
//
//        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
//    }

    // 400 - Bad Request
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
}
