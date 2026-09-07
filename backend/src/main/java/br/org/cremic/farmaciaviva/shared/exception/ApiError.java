package br.org.cremic.farmaciaviva.shared.exception;

import java.time.OffsetDateTime;
import java.util.List;

public record ApiError(
    OffsetDateTime timestamp,
    int status,
    String error,
    String message,
    String path,
    List<FieldValidationError> fieldErrors
) {

    public record FieldValidationError(String field, String message) {
    }

    public static ApiError of(
        OffsetDateTime timestamp,
        int status,
        String error,
        String message,
        String path
    ) {
        return new ApiError(timestamp, status, error, message, path, List.of());
    }
}
