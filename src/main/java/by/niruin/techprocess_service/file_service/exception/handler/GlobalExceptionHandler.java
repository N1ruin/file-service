package by.niruin.techprocess_service.file_service.exception.handler;

import by.niruin.techprocess_service.file_service.exception.*;
import by.niruin.techprocess_service.file_service.model.error.ErrorResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LogManager.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(InvalidFileNameException.class)
    public ResponseEntity<ErrorResponse> handleInvalidFileName(InvalidFileNameException exception) {
        log.warn("Exception: {}", exception.getMessage(), exception);

        var errorResponse = new ErrorResponse("Invalid File Name", "The file name contains unsupported " +
                "characters or format", HttpStatus.BAD_REQUEST.value());

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(FileDeleteException.class)
    public ResponseEntity<ErrorResponse> handleDeleteFileException(FileDeleteException exception) {
        log.error("File deletion error: {}", exception.getMessage(), exception);

        var errorResponse = new ErrorResponse("Deletion Failed", "Unable to delete the file. Please try again",
                HttpStatus.INTERNAL_SERVER_ERROR.value());

        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(InvalidFileFormatException.class)
    public ResponseEntity<ErrorResponse> handleInvalidImageFormat(InvalidFileFormatException exception) {
        log.warn("Invalid image format: {}", exception.getMessage(), exception);

        var errorResponse = new ErrorResponse("Unsupported File Format", "The file format is not supported." +
                " Please use a valid format", HttpStatus.BAD_REQUEST.value());

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(FileUploadException.class)
    public ResponseEntity<ErrorResponse> handleFileUpload(FileUploadException exception) {
        log.warn("File upload error: {}", exception.getMessage(), exception);

        var errorResponse = new ErrorResponse("Upload Failed", "Unable to upload the file. " +
                "Please check your request and try again", HttpStatus.INTERNAL_SERVER_ERROR.value());

        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(FileDownloadException.class)
    public ResponseEntity<ErrorResponse> handleFileDownload(FileDownloadException exception) {
        log.warn("File download error: {}", exception.getMessage(), exception);

        var errorResponse = new ErrorResponse("Download Failed", "Unable to download the file. Please try again",
                HttpStatus.INTERNAL_SERVER_ERROR.value());

        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(FileNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleFileNotFound(FileNotFoundException exception) {
        log.warn("File not found error: {}", exception.getMessage(), exception);

        var errorResponse = new ErrorResponse("File Not Found", "The requested file does not exist or has been removed",
                HttpStatus.NOT_FOUND.value());

        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(FileTooLargeException.class)
    public ResponseEntity<ErrorResponse> handleFileTooLarge(FileTooLargeException exception) {
        log.warn("File too large error: {}", exception.getMessage(), exception);

        var errorResponse = new ErrorResponse("File Too Large", "The file exceeds the maximum allowed size",
                HttpStatus.CONTENT_TOO_LARGE.value());

        return new ResponseEntity<>(errorResponse, HttpStatus.CONTENT_TOO_LARGE);
    }

    @ExceptionHandler(FileUpdateException.class)
    public ResponseEntity<ErrorResponse> handleFileUpdate(FileUpdateException exception) {
        log.warn("File update error: {}", exception.getMessage(), exception);

        var errorResponse = new ErrorResponse("Update Failed", "Unable to update the file. Please try again",
                HttpStatus.INTERNAL_SERVER_ERROR.value());

        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleMethodValidation(HandlerMethodValidationException exception) {
        log.warn("Parameter validation error: {}", exception.getMessage(), exception);

        var errorResponse = new ErrorResponse("Validation Error", "Invalid request parameters." +
                " Please check your input", HttpStatus.BAD_REQUEST.value());

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception exception) {
        log.error("Unknown exception occurred: {}", exception.getMessage(), exception);

        var errorResponse = new ErrorResponse("Internal Server Error",
                "An unexpected error occurred. Please try again later.",
                HttpStatus.INTERNAL_SERVER_ERROR.value());

        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
