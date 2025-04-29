package com.example.domain.api.ans_api_module.correction_answer.rule;

//TODO данный механизм должен использовать GROOVY скрипты
public interface RewriteRule {

    /**
     * Проверяет, применимо ли данное правило к тексту.
     * @param text Текст для проверки (обычно, скорректированный текст).
     * @return true, если правило должно быть применено, иначе false.
     */
    boolean shouldApply(String text);

    /**
     * Возвращает специфичную инструкцию для переписывания, если правило применимо.
     * @param originalText Текст, к которому применяется правило. Может быть использован для генерации динамической инструкции.
     * @return Инструкция для добавления к промпту для переписывания.
     */
    String getInstruction(String originalText); // Инструкция теперь может быть динамической
}
