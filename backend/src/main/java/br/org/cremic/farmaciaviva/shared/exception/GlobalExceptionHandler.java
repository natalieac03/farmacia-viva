package br.org.cremic.farmaciaviva.shared.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final Clock clock;

    public GlobalExceptionHandler(Clock clock) {
        this.clock = clock;
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleResourceNotFound(
        ResourceNotFoundException ex,
        HttpServletRequest request
    ) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ApiError> handleBusinessRule(
        BusinessRuleException ex,
        HttpServletRequest request
    ) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), request);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiError> handleDuplicateResource(
        DuplicateResourceException ex,
        HttpServletRequest request
    ) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrityViolation(
        DataIntegrityViolationException ex,
        HttpServletRequest request
    ) {
        log.warn("Violacao de integridade em {} {}",
            request.getMethod(), request.getRequestURI(), ex);
        return build(HttpStatus.CONFLICT, "Violacao de integridade de dados", request);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ApiError> handleOptimisticLocking(
        OptimisticLockingFailureException ex,
        HttpServletRequest request
    ) {
        return build(
            HttpStatus.CONFLICT,
            "O registro foi alterado por outra operacao. Recarregue os dados e tente novamente",
            request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(
        MethodArgumentNotValidException ex,
        HttpServletRequest request
    ) {
        List<ApiError.FieldValidationError> fieldErrors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(fieldError -> new ApiError.FieldValidationError(
                fieldError.getField(),
                fieldError.getDefaultMessage()))
            .toList();

        return buildComCampos(
            HttpStatus.BAD_REQUEST,
            "Erro de validacao nos dados enviados",
            request,
            fieldErrors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(
        ConstraintViolationException ex,
        HttpServletRequest request
    ) {
        List<ApiError.FieldValidationError> fieldErrors = ex.getConstraintViolations()
            .stream()
            .map(violation -> new ApiError.FieldValidationError(
                violation.getPropertyPath().toString(),
                violation.getMessage()))
            .toList();

        return buildComCampos(
            HttpStatus.BAD_REQUEST,
            "Erro de validacao nos dados enviados",
            request,
            fieldErrors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleMessageNotReadable(
        HttpMessageNotReadableException ex,
        HttpServletRequest request
    ) {
        return build(
            HttpStatus.BAD_REQUEST,
            "Corpo da requisicao invalido ou mal formatado",
            request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(
        MethodArgumentTypeMismatchException ex,
        HttpServletRequest request
    ) {
        String message = "Parametro '" + ex.getName() + "' possui valor invalido";
        return build(HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> handleMissingParameter(
        MissingServletRequestParameterException ex,
        HttpServletRequest request
    ) {
        String message = "Parametro obrigatorio ausente: " + ex.getParameterName();
        return build(HttpStatus.BAD_REQUEST, message, request);
    }

    /**
     * Ordenacao invalida na paginacao (por exemplo sort=campoInexistente) chega
     * como PropertyReferenceException e representa um erro do cliente, nao uma
     * falha do servidor.
     */
    @ExceptionHandler(PropertyReferenceException.class)
    public ResponseEntity<ApiError> handlePropertyReference(
        PropertyReferenceException ex,
        HttpServletRequest request
    ) {
        String message = "Campo de ordenacao invalido: " + ex.getPropertyName();
        return build(HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> handleMethodNotSupported(
        HttpRequestMethodNotSupportedException ex,
        HttpServletRequest request
    ) {
        String message = "Metodo " + ex.getMethod() + " nao suportado para este recurso";
        return build(HttpStatus.METHOD_NOT_ALLOWED, message, request);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiError> handleMediaTypeNotSupported(
        HttpMediaTypeNotSupportedException ex,
        HttpServletRequest request
    ) {
        return build(
            HttpStatus.UNSUPPORTED_MEDIA_TYPE,
            "Tipo de conteudo nao suportado. Utilize application/json",
            request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleNoResourceFound(
        NoResourceFoundException ex,
        HttpServletRequest request
    ) {
        return build(HttpStatus.NOT_FOUND, "Recurso nao encontrado", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(
        Exception ex,
        HttpServletRequest request
    ) {
        log.error("Erro inesperado ao processar {} {}",
            request.getMethod(), request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno inesperado", request);
    }

    private ResponseEntity<ApiError> build(
        HttpStatus status,
        String message,
        HttpServletRequest request
    ) {
        ApiError body = ApiError.of(
            OffsetDateTime.now(clock),
            status.value(),
            status.getReasonPhrase(),
            message,
            request.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }

    private ResponseEntity<ApiError> buildComCampos(
        HttpStatus status,
        String message,
        HttpServletRequest request,
        List<ApiError.FieldValidationError> fieldErrors
    ) {
        ApiError body = new ApiError(
            OffsetDateTime.now(clock),
            status.value(),
            status.getReasonPhrase(),
            message,
            request.getRequestURI(),
            fieldErrors);
        return ResponseEntity.status(status).body(body);
    }
}
