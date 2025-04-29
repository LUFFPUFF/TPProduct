package com.example.domain.api.ans_api_module.answer_finder.nlp.lemmatizer.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class UdpipeResponse {

    private String result;
    private double runtime;
    private String error;

    public boolean hasError() {
        return error != null && !error.isEmpty();
    }

    public String getErrorMessage() {
        return error;
    }
}
