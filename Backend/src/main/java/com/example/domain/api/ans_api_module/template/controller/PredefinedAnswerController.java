package com.example.domain.api.ans_api_module.template.controller;

import com.example.domain.api.ans_api_module.template.services.answer.FileProcessorService;
import com.example.domain.api.ans_api_module.template.services.answer.PredefinedAnswerService;
import com.example.domain.dto.ans_module.predefined_answer.request.PredefinedAnswerUploadDto;
import com.example.domain.dto.ans_module.predefined_answer.request.UploadFileRequest;
import com.example.domain.dto.ans_module.predefined_answer.response.AnswerResponse;
import com.example.domain.dto.ans_module.predefined_answer.response.UploadResultResponse;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/answers")
@RequiredArgsConstructor
public class PredefinedAnswerController {

    private final PredefinedAnswerService answerService;
    private final FileProcessorService fileProcessorService;

    @PostMapping
    @Transactional
    public ResponseEntity<AnswerResponse> createAnswer(@Valid @RequestBody PredefinedAnswerUploadDto dto) {
        AnswerResponse response = answerService.createAnswer(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<AnswerResponse> updateAnswer(
            @PathVariable Integer id,
            @Valid @RequestBody PredefinedAnswerUploadDto dto) {
        AnswerResponse response = answerService.updateAnswer(id, dto);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> deleteAnswer(@PathVariable Integer id) {
        answerService.deleteAnswer(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<AnswerResponse> getAnswerById(@PathVariable Integer id) {
        AnswerResponse response = answerService.getAnswerById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<Page<AnswerResponse>> searchAnswers(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Integer companyId,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<AnswerResponse> response = answerService.searchAnswers(query, companyId, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<AnswerResponse>> getAnswersByCategory(
            @PathVariable String category,
            @RequestParam(required = false) Integer companyId) {
        List<AnswerResponse> responses = companyId != null
                ? (List<AnswerResponse>) answerService.getAnswerById(companyId)
                : answerService.getAnswersByCategory(category);
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/upload")
    public ResponseEntity<UploadResultResponse> uploadAnswers(
            @RequestParam("file") MultipartFile file,
            @RequestParam Integer companyId,
            @RequestParam String category,
            @RequestParam(defaultValue = "false") boolean overwrite) {

        UploadFileRequest request = UploadFileRequest.builder()
                .file(file)
                .companyId(companyId)
                .category(category)
                .overwriteExisting(overwrite)
                .build();

        UploadResultResponse response = fileProcessorService.processFileUpload(request);

        return response.getStatus().equals("FAILED")
                ? ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response)
                : ResponseEntity.ok(response);
    }

    @DeleteMapping("/batch")
    @Transactional
    public ResponseEntity<Void> deleteByCompanyAndCategory(
            @RequestParam Integer companyId,
            @RequestParam String category) {
        answerService.deleteByCompanyIdAndCategory(companyId, category);
        return ResponseEntity.noContent().build();
    }


}
