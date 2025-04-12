package com.example.domain.api.ans_api_module.template.services.answer;

import com.example.domain.api.ans_api_module.template.dto.request.PredefinedAnswerUploadDto;
import com.example.domain.api.ans_api_module.template.dto.response.AnswerResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PredefinedAnswerService {

    AnswerResponse createAnswer(PredefinedAnswerUploadDto dto);
    AnswerResponse updateAnswer(Integer id, PredefinedAnswerUploadDto dto);
    void deleteAnswer(Integer id);
    int deleteByCompanyIdAndCategory(Integer companyId, String category);
    List<AnswerResponse> getAllAnswers();
    AnswerResponse getAnswerById(Integer id);
    Page<AnswerResponse> searchAnswers(String query, Integer companyId, Pageable pageable);
    List<AnswerResponse> getAnswersByCategory(String category);
}
