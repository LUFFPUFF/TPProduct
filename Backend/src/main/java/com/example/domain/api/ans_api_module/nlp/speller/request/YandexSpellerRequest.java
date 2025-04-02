package com.example.domain.api.ans_api_module.nlp.speller.request;

import lombok.Data;

@Data
public class YandexSpellerRequest {

    private final String text;
    private final String lang;
}
