package nz.waiwatts.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleAllExceptions(Exception ex, WebRequest request) {
        String requestId = MDC.get("requestId");
        
        // Special logging for CORS-related issues
        if (ex.getMessage() != null && ex.getMessage().contains("CORS")) {
            logger.warn("CORS configuration issue detected - requestId: {} - message: {}", 
                requestId, ex.getMessage());
        } else {
            logger.error("Unhandled exception - requestId: {} - type: {} - message: {}", 
                requestId, ex.getClass().getSimpleName(), ex.getMessage(), ex);
        }
        
        Map<String, String> errorResponse = Map.of(
            "error", "Internal server error",
            "message", "An unexpected error occurred",
            "requestId", requestId != null ? requestId : "unknown"
        );
        
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(errorResponse);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex, WebRequest request) {
        String requestId = MDC.get("requestId");
        
        logger.warn("Malformed request - requestId: {} - message: {}", requestId, ex.getMessage());
        
        Map<String, String> errorResponse = Map.of(
            "error", "Bad request",
            "message", "Malformed JSON request",
            "requestId", requestId != null ? requestId : "unknown"
        );
        
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(errorResponse);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgumentException(IllegalArgumentException ex, WebRequest request) {
        String requestId = MDC.get("requestId");
        
        logger.warn("Invalid argument - requestId: {} - message: {}", requestId, ex.getMessage());
        
        Map<String, String> errorResponse = Map.of(
            "error", "Bad request",
            "message", ex.getMessage(),
            "requestId", requestId != null ? requestId : "unknown"
        );
        
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(errorResponse);
    }
}