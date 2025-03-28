package com.example.domain.api.ans_api_module.nlp.punctuation_rule.correction_models;

import java.util.List;

public record CorrectedText(String text, List<Correction> corrections) {}

