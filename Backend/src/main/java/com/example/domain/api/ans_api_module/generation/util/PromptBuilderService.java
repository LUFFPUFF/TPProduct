package com.example.domain.api.ans_api_module.generation.util;


import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PromptBuilderService {

    private static final int TARGET_MAX_REPLY_LENGTH_CHARS = 300;
    private static final int TARGET_REWRITE_MAX_LENGTH_CHARS = 200;

    public static String buildCorrectionPrompt(String originalText) {
        if (originalText == null || originalText.trim().isEmpty()) {
            return "";
        }

        return String.format(
                "Задание: Исправь все грамматические, орфографические и пунктуационные ошибки в следующем тексте. " +
                        "Сохрани исходный смысл и стиль. Выведи только исправленный текст.\n" +
                        "Текст для исправления: \"%s\"",
                sanitizeForModel(originalText)
        );
    }

    public static String buildRewritePrompt(String originalAnswerText, String clientStyleExample) {
        if (originalAnswerText == null || originalAnswerText.trim().isEmpty()) {
            return "";
        }

        String styleInstruction;
        if (clientStyleExample != null && !clientStyleExample.trim().isEmpty()) {
            styleInstruction = String.format(
                    "Адаптируй ответ под стиль общения клиента. Стиль клиента: \"%s\". ",
                    sanitizeForModel(clientStyleExample)
            );
        } else {
            styleInstruction = "Сделай ответ более теплым, эмпатичным и дружелюбным. Избегай формальностей и канцеляризмов. ";
        }

        return "Задание: Перепиши следующий ответ так, чтобы он был максимально полезным и понятным для клиента. " +
                styleInstruction +
                "Ответ должен быть кратким, в пределах " + TARGET_REWRITE_MAX_LENGTH_CHARS + " символов. " +
                "Выведи строго только переписанный текст ответа, без каких-либо вступлений или пояснений.\n" +
                "Исходный ответ для переработки: \"" + sanitizeForModel(originalAnswerText) + "\"";
    }

    public static String buildGeneralAnswerPrompt(String userQuery, String companyDescription, String clientPreviousMessages) {
        if (userQuery == null || userQuery.trim().isEmpty()) {
            return "";
        }
        if (companyDescription == null || companyDescription.trim().isEmpty()) {
            log.warn("Company description is missing for general answer generation. Model might not be able to answer company-specific questions.");
        }

        String rolePlay = "Ты — дружелюбный и компетентный ИИ-ассистент компании. Твоя главная задача — помочь клиенту, предоставив точный и полезный ответ.";

        String contextInstruction = String.format(
                "Используй предоставленную ниже информацию о компании, чтобы полно и точно ответить на вопрос клиента. " +
                        "Если вопрос выходит за рамки деятельности компании или информации нет в описании, вежливо сообщи об этом. " +
                        "Не придумывай информацию. Ответ должен быть в пределах %d символов.",
                TARGET_MAX_REPLY_LENGTH_CHARS
        );

        String styleInstruction = "Старайся отвечать в позитивном и поддерживающем ключе. ";
        if (clientPreviousMessages != null && !clientPreviousMessages.trim().isEmpty()) {
            styleInstruction += String.format(
                    "Учитывай предыдущие сообщения клиента для поддержания контекста и стиля диалога. " +
                            "Вот фрагмент предыдущего общения с клиентом (используй его для понимания стиля и тона клиента): \n\"%s\". ",
                    sanitizeForModel(clientPreviousMessages.substring(0, Math.min(clientPreviousMessages.length(), 500)))
            );
        } else {
            styleInstruction += "Будь вежлив и профессионален. ";
        }

        styleInstruction += "Формулируй ответ так, чтобы он был максимально ясным для обычного пользователя.";

        StringBuilder prompt = new StringBuilder();
        prompt.append(rolePlay).append("\n\n");
        prompt.append("ИНСТРУКЦИИ ДЛЯ ОТВЕТА:\n");
        prompt.append(contextInstruction).append("\n");
        prompt.append(styleInstruction).append("\n\n");

        if (companyDescription != null && !companyDescription.trim().isEmpty()) {
            prompt.append("ИНФОРМАЦИЯ О КОМПАНИИ (используй только эту информацию для ответов о компании):\n");
            prompt.append(sanitizeForModel(companyDescription)).append("\n\n");
        } else {
            prompt.append("ПРЕДУПРЕЖДЕНИЕ: Информация о компании не предоставлена. Ты не сможешь отвечать на вопросы, связанные с деятельностью компании.\n\n");
        }

        prompt.append("ВОПРОС КЛИЕНТА:\n");
        prompt.append(sanitizeForModel(userQuery)).append("\n\n");
        prompt.append("ТВОЙ ОТВЕТ (краткий, точный, дружелюбный, в пределах указанной длины, только сам текст ответа):");

        return prompt.toString();
    }

    public static String buildClientStyleAnalysisPrompt(String clientMessages) {
        if (clientMessages == null || clientMessages.trim().isEmpty()) {
            return "";
        }

        String example1ClientText = "Привет! Слушайте, у меня тут проблемка с заказом #123. Не могли бы вы по-быстрому чекнуть, чё там с доставкой? А то уже жду не дождусь (((";
        String example1StyleDescription = "Неформальный, разговорный, использует сокращения (\"чё\") и просторечные обороты, дружелюбный тон, выражает эмоции (смайлик, нетерпение). Предпочитает быстрые и прямые ответы.";

        String example2ClientText = "Добрый день. Прошу предоставить информацию о статусе рассмотрения моей заявки № Z-4567, поданной 15.03.2024. Требуется официальный ответ в установленные сроки.";
        String example2StyleDescription = "Формальный, деловой, структурированный, использует официальные формулировки, ожидает конкретный ответ, тональность нейтрально-требовательная.";

        String example3ClientText = "блин ну где мой заказ???? УЖЕ НЕДЕЛЮ жду!!!!!!!!!! СКОЛЬКО МОЖНО ИЗДЕВАТЬСЯ НАД ЛЮДЬМИ ВЫ ВООБЩЕ РАБОТАЕТЕ ТАМ???????????";
        String example3StyleDescription = "Крайне неформальный, очень эмоциональный (раздражение, гнев), использует капслок и множество вопросительных/восклицательных знаков, требовательный, ожидает немедленного решения проблемы.";


        return "Задание: Проанализируй стиль общения клиента на основе предоставленного текста (или нескольких последних сообщений) и дай краткое, но емкое описание этого стиля. " +
                "Описание должно помочь понять, как лучше общаться с этим клиентом, чтобы ответ был воспринят позитивно. " +
                "Укажи на формальность, тональность, эмоциональность, краткость/многословность и использование специфических языковых средств.\n\n" +
                "Пример 1:\n" +
                "Текст клиента: \"" + sanitizeForModel(example1ClientText) + "\"\n" +
                "Описание стиля: " + example1StyleDescription + "\n\n" +
                "Пример 2:\n" +
                "Текст клиента: \"" + sanitizeForModel(example2ClientText) + "\"\n" +
                "Описание стиля: " + example2StyleDescription + "\n\n" +
                "Пример 3:\n" +
                "Текст клиента: \"" + sanitizeForModel(example3ClientText) + "\"\n" +
                "Описание стиля: " + example3StyleDescription + "\n\n" +
                "Текст клиента для анализа:\n" +
                "\"" + sanitizeForModel(clientMessages) + "\"\n\n" +
                "Описание стиля (будь кратким и по существу):";
    }

    private static String sanitizeForModel(String input) {
        if (input == null) {
            return "";
        }

        String sanitized = input.replace("\"", "'");
        sanitized = sanitized.replace("\n", " ").replace("\r", " ").trim();
        return sanitized;
    }
}
