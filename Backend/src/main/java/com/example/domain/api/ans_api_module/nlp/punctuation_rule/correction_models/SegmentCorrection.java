package com.example.domain.api.ans_api_module.nlp.punctuation_rule.correction_models;

import java.util.List;

public record SegmentCorrection(TextSegment segment,
                                String correctedText,
                                List<Correction> corrections) {}
