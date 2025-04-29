package com.example.domain.api.ans_api_module.template.services.answer;

import com.example.domain.api.ans_api_module.template.util.FileType;
import com.example.domain.api.ans_api_module.template.dto.request.UploadFileRequest;
import com.example.domain.api.ans_api_module.template.dto.response.UploadResultResponse;
import org.springframework.web.multipart.MultipartFile;

public interface FileProcessorService {

    UploadResultResponse processFileUpload(UploadFileRequest request);
    void validateFile(MultipartFile file);
    FileType detectFileType(MultipartFile file);
}
