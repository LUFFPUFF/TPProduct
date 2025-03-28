package com.example.domain.api.ans_api_module.nlp.punctuation_rule.correction_models;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter @Setter
@RequiredArgsConstructor
public class Correction {

    private final int position;
    private final String original;
    private final String corrected;
    private final CorrectionType type;

    public Correction withAdjustedPosition(int offset) {
        return new Correction(
                this.position + offset,
                this.original,
                this.corrected,
                this.type
        );
    }

}
