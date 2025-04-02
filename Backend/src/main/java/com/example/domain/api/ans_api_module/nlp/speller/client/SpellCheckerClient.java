package com.example.domain.api.ans_api_module.nlp.speller.client;

import com.example.domain.api.ans_api_module.nlp.speller.request.YandexSpellerRequest;
import com.example.domain.api.ans_api_module.nlp.speller.response.SpellerResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class SpellCheckerClient {

    private static final String YANDEX_SPELLER_URL = "https://speller.yandex.net/services/spellservice.json/checkText";

    private final RestTemplate restTemplate;

    public SpellCheckerClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public List<SpellerResponse> checkText(String text, String lang) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        YandexSpellerRequest yandexRequest = new YandexSpellerRequest(text, lang);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("text", yandexRequest.getText());
        map.add("lang", yandexRequest.getLang());

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, httpHeaders);
        ResponseEntity<SpellerResponse[]> response = restTemplate.exchange(
                YANDEX_SPELLER_URL, HttpMethod.POST, request, SpellerResponse[].class
        );

        response.getBody();
        return List.of(response.getBody());
    }
}
