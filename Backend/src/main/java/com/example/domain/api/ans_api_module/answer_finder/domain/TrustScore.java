package com.example.domain.api.ans_api_module.answer_finder.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Embeddable;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@NoArgsConstructor
public class TrustScore {

    private double score;

    @JsonCreator
    public TrustScore(@JsonProperty("score") double score) {
        if (score < 0.0 || score > 1.0) {
            throw new IllegalArgumentException("Trust score must be between 0.0 and 1.0");
        }
        this.score = score;
    }
}
