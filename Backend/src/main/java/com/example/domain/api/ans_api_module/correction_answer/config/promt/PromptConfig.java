package com.example.domain.api.ans_api_module.correction_answer.config.promt;

import java.util.Objects;

public record PromptConfig(
        String correctionPromptTemplate,
        String rewriteInstructionTemplate,
        String rewritePromptTemplate) {

    public PromptConfig {
        Objects.requireNonNull(correctionPromptTemplate, "Correction template cannot be null");
        Objects.requireNonNull(rewriteInstructionTemplate, "Rewrite instruction cannot be null");
        Objects.requireNonNull(rewritePromptTemplate, "Rewrite template cannot be null");

        if (correctionPromptTemplate.isBlank() || rewritePromptTemplate.isBlank()) {
            throw new IllegalArgumentException("Prompt templates cannot be blank");
        }
    }

    public PromptConfig withTemplates(String correction, String instruction, String rewrite) {
        return new PromptConfig(
                correction != null ? correction : this.correctionPromptTemplate,
                instruction != null ? instruction : this.rewriteInstructionTemplate,
                rewrite != null ? rewrite : this.rewritePromptTemplate
        );
    }

    public static PromptConfig getDefault() {
        return new PromptConfig(
                "Исправь орфографические, пунктуационные и грамматические ошибки в следующем предложении, \n" +
                        "  строго соблюдая нормы современного русского языка. Дай только исправленный вариант без пояснений: %s",
                "Перепиши следующий корпоративный ответ, сделав его более дружелюбным, теплым и естественным, но при этом не перегруженным деталями. Сделай так, \n" +
                        "  чтобы ответ звучал приветливо и было ощущение личного общения. Также убедись, что ответ остается коротким и по делу. ",
                "%sШаблонный ответ: %s. Дай только измененный вариант."
        );
    }
}
