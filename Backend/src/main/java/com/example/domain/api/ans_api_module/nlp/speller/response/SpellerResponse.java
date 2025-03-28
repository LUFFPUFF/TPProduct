package com.example.domain.api.ans_api_module.nlp.speller.response;

import lombok.Data;

import java.util.List;

@Data
public class SpellerResponse {
    private int code;
    private int pos;
    private int row;
    private int col;
    private int len;
    private String word;
    private List<String> s;
}
