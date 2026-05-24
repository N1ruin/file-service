package by.niruin.techprocess_service.file_service.exception;

public class FileAlreadyExistException extends RuntimeException {
    public FileAlreadyExistException(String message) {
        super(message);
    }
}
