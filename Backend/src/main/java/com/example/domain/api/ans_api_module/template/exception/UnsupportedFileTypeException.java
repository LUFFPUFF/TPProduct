package com.example.domain.api.ans_api_module.template.exception;

import com.example.domain.api.ans_api_module.template.util.FileType;

public class UnsupportedFileTypeException extends RuntimeException {

    public UnsupportedFileTypeException(FileType fileType) {
        super(STR."Unsupported file type: \{fileType}");
    }

    public UnsupportedFileTypeException() {
    }

    public UnsupportedFileTypeException(String message) {
        super(message);
    }

    public UnsupportedFileTypeException(String message, Throwable cause) {
        super(message, cause);
    }
}
