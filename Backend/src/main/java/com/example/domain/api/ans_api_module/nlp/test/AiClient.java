package com.example.domain.api.ans_api_module.nlp.test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;


public class AiClient {

    private static final String API_URL = "http://localhost:5000/generate";

    public static void main(String[] args) {
        AiClient aiClient = new AiClient();

        String text = "ПриветкакделаЯзавтраприеду.тысможешьменявстретить";
        String prompt = "Напиши обход в ширину на Java и уложись в 50 симвлов";
        String response = aiClient.getResponse(prompt);


        System.out.println("Ответ от сервера: " + response);
    }

    public String getResponse(String prompt) {
        OkHttpClient client = new OkHttpClient();
        ObjectMapper objectMapper = new ObjectMapper();

        String json = String.format("{\"prompt\": \"%s\", \"max_new_tokens\": 100}", prompt);

        RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));

        Request request = new Request.Builder()
                .url(API_URL)
                .post(body)
                .build();

        String responseBody = "";

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String rawResponse = new String(response.body().bytes(), StandardCharsets.UTF_8);

                JsonNode rootNode = objectMapper.readTree(rawResponse);
                return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootNode);
            } else {
                System.out.println("Ошибка: " + response.code() + " " + response.message());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return responseBody;
    }


}
