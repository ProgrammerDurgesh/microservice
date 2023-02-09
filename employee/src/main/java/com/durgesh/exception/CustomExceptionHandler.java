package com.durgesh.exception;

import com.durgesh.response.CustomResponse;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLIntegrityConstraintViolationException;

@RestControllerAdvice
public class CustomExceptionHandler extends CustomResponse {
    @ExceptionHandler(value = ChangeSetPersister.NotFoundException.class)
    @ResponseBody
    public ResponseEntity<?> userNotFoundException() {
        return response("User Not Found", HttpStatus.NOT_FOUND,"User Not Found");
    }
    @ExceptionHandler(value = NullPointerException.class)
    @ResponseBody
    public ResponseEntity<?> nullPointerException() {
        return response("NULL Pointer Exception", HttpStatus.INTERNAL_SERVER_ERROR, "null");
    }
    @ExceptionHandler(value = SQLIntegrityConstraintViolationException.class)
    public ResponseEntity<?> sqlException() {
        return response("Record Already Exist", HttpStatus.INTERNAL_SERVER_ERROR, "SQL Error");
    }
    @ExceptionHandler(value = MissingPathVariableException.class)
    public ResponseEntity<?> missingPathVariableException() {
        return response("@PathVariable Incorrect", HttpStatus.INTERNAL_SERVER_ERROR, "500");
    }
    @ExceptionHandler(value = HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<?> httpRequestMethodNotSupportedException() {
        return response("Request Method Not Allowed", HttpStatus.METHOD_NOT_ALLOWED, "405");
    }

}
