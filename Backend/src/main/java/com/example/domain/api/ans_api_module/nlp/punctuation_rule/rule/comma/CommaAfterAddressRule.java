package com.example.domain.api.ans_api_module.nlp.punctuation_rule.rule.comma;

import com.example.domain.api.ans_api_module.nlp.punctuation_rule.correction.TextAnalysis;
import com.example.domain.api.ans_api_module.nlp.punctuation_rule.rule.CorrectionRule;

import java.util.Optional;
import java.util.Set;

public class CommaAfterAddressRule extends CorrectionRule {


    public static void main(String[] args) {

        CorrectionRule correctionRule = new CommaAfterAddressRule();

    }

    private static final Set<String> ADDRESS_WORDS = Set.of(
            "привет", "здравствуй", "здравствуйте", "добрый день",
            "доброе утро", "добрый вечер", "дорогой", "уважаемый",
            "милый", "друг", "подруга"
    );

    public CommaAfterAddressRule() {
        super("", true);
    }

    @Override
    public String apply(String text, TextAnalysis analysis) {
        return findAddressWord(text)
                .filter(addressInfo -> needsComma(text, addressInfo))
                .map(addressInfo -> insertComma(text, addressInfo))
                .orElse(text);
    }

    private Optional<AddressInfo> findAddressWord(String text) {
        String lowerText = text.toLowerCase();

        for (String addressWord : ADDRESS_WORDS) {
            if (lowerText.startsWith(addressWord.toLowerCase())) {
                int addressLength = addressWord.length();
                return Optional.of(new AddressInfo(
                        text.substring(0, addressLength),
                        addressLength
                ));
            }
        }
        return Optional.empty();
    }

    private boolean needsComma(String text, AddressInfo addressInfo) {
        if (text.length() <= addressInfo.length()) {
            return false;
        }

        String rest = text.substring(addressInfo.length()).stripLeading();
        return !rest.isEmpty() && !rest.startsWith(",");
    }

    private String insertComma(String text, AddressInfo addressInfo) {
        String rest = text.substring(addressInfo.length());
        return STR."\{addressInfo.word()},\{rest}";
    }

    private record AddressInfo(String word, int length) {}
}
