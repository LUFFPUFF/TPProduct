package com.example.domain.api.ans_api_module.correction_answer.rule;

public class TestRule implements RewriteRule{

    @Override
    public boolean shouldApply(String text) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getInstruction(String originalText) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
