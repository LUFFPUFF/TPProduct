package com.example.domain.api.ans_api_module.template.batch.item;

import com.example.domain.api.ans_api_module.template.util.FileType;
import com.example.domain.dto.ans_module.predefined_answer.request.PredefinedAnswerUploadDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface AnswerFileReader {

    List<PredefinedAnswerUploadDto> read(MultipartFile file);
    boolean supports(FileType fileType);

}
