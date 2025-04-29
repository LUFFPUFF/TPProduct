package com.example.domain.api.ans_api_module.correction_answer.config.promt;

import com.example.domain.api.ans_api_module.correction_answer.config.yaml.YamlPropertySourceFactory;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@ConfigurationProperties("prompt")
@PropertySource(value = "classpath:prompt.yml", factory = YamlPropertySourceFactory.class)
@Data
public class PromptTemplateProperties {

    private String correctionPromptTemplate;
    private String rewriteInstructionTemplate;
    private String rewritePromptTemplate;

    public static PromptTemplateProperties getDefault() {
        return new PromptTemplateProperties();
    }
}
