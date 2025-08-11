package ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GenericResponseDto<T> {
    private String timestamp;
    private int status;
    private String error;
    private String message;
    private T data;

    public static <T> GenericResponseDto<T> success(String message, T data, int status) {
        return new GenericResponseDto<>(
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                status,
                null,
                message,
                data
        );
    }

    public static <T> GenericResponseDto<T> error(String error, String message, int status) {
        return new GenericResponseDto<>(
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                status,
                error,
                message,
                null
        );
    }
}
