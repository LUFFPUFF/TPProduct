package com.example.domain.api.chat_service_api.integration.whats_app.controller;

import com.example.database.repository.company_subscription_module.CompanyWhatsappConfigurationRepository;
import com.example.domain.api.chat_service_api.integration.whats_app.WhatsappWebhookProcessor;
import com.example.domain.api.chat_service_api.integration.whats_app.model.request.WhatsappWebhookRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/webhook/whatsapp")
@RequiredArgsConstructor
@Slf4j
public class WhatsappWebhookController {

    private final WhatsappWebhookProcessor whatsappWebhookProcessor;
    private final CompanyWhatsappConfigurationRepository whatsappConfigurationRepository;

    /**
     * GET метод для верификации Webhook в Meta Developer Portal.
     * Принимает challenge токен и echo'ит его обратно.
     * Параметры: hub.mode=subscribe, hub.challenge, hub.verify_token
     */
    @GetMapping
    public ResponseEntity<String> verifyWebhook(@RequestParam("hub.mode") String mode,
                                                @RequestParam("hub.challenge") String challenge,
                                                @RequestParam("hub.verify_token") String verifyToken) {
        log.info("Webhook verification request received. Mode: {}, Challenge: {}, Verify Token: {}",
                mode, challenge, verifyToken);

        boolean tokenIsValid = whatsappConfigurationRepository.findAll().stream()
                .anyMatch(config -> config.getVerifyToken() != null && config.getVerifyToken().equals(verifyToken));

        if ("subscribe".equals(mode) && tokenIsValid) {
            log.info("Webhook verification successful. Returning challenge.");
            return ResponseEntity.ok(challenge);
        } else {
            log.error("Webhook verification failed. Mode: {}, Token valid: {}", mode, tokenIsValid);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Verification failed");
        }
    }

    /**
     * POST метод для приема входящих сообщений и статусов от Meta.
     * Тело запроса - JSON структура от Meta.
     */
    @PostMapping
    public ResponseEntity<Void> handleWebhook(@RequestBody WhatsappWebhookRequest request) {
        try {
            whatsappWebhookProcessor.process(request);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error processing webhook request", e);
            return ResponseEntity.ok().build();
        }
    }
}
