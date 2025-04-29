package com.example.domain.api.ans_api_module.template.batch.item;

import com.example.domain.api.ans_api_module.template.util.FileType;
import com.example.domain.api.ans_api_module.template.dto.request.PredefinedAnswerUploadDto;

import java.io.File;

import java.util.List;

public interface AnswerFileReader {

    List<PredefinedAnswerUploadDto> read(File file);
    boolean supports(FileType fileType);

}
