package com.example.domain.api.ans_api_module.answer_finder.nlp.lemmatizer.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
@Setter
@ConfigurationProperties(prefix = "nlp.lemmatizer")
public class NlpProperties {

    private String implementation = "external-api";
    private String apiUrl = "https://lindat.mff.cuni.cz/services/udpipe/api/process";
    private String udpipeModel = "russian-gsd-ud-2.15-241121";
    private String udpipeTokenizer = "horizontal";
    private String udpipeTagger = "horizontal";
    private String udpipeParser = "none";
    private String udpipeOutput = "conllu";

    private int apiBatchLimit = 1000;


    private int connectTimeoutMillis = 5000;
    private int readTimeoutMillis = 10000;
}
