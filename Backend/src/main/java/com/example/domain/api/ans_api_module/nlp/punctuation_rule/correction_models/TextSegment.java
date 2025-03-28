package com.example.domain.api.ans_api_module.nlp.punctuation_rule.correction_models;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
public class TextSegment {

    private final int startPos;
    private final int endPos;
    private final String text;


}
