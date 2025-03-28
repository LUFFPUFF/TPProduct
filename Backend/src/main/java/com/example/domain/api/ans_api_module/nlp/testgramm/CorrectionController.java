package com.example.domain.api.ans_api_module.nlp.testgramm;

import com.example.domain.api.ans_api_module.nlp.punctuation_rule.correction_models.CorrectedText;
import com.example.domain.api.ans_api_module.nlp.speller.service.GrammarCorrectionService;
import io.micrometer.core.annotation.Timed;
import lombok.Data;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/correction")
public class CorrectionController {

    private final GrammarCorrectionService correctionService;

    public CorrectionController(GrammarCorrectionService correctionService) {
        this.correctionService = correctionService;
    }

    @PostMapping("/correct")
    @Timed(value = "correction.api.time", description = "Time spent processing correction requests")
    public CorrectedText correctText(@RequestBody CorrectionRequest request) {
        return correctionService.correctText(request.getText(), request.getLang());
    }

    @Data
    public static class CorrectionRequest {
        private String text;
        private String lang;
    }
}
