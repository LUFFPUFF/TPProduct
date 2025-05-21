package com.example.domain.api.statistics_module.model;

import com.example.domain.api.statistics_module.model.auth.AuthSummaryStatsDTO;
import com.example.domain.api.statistics_module.model.auth.RegistrationSummaryStatsDTO;
import com.example.domain.api.statistics_module.model.auth.RoleSummaryStatsDTO;
import com.example.domain.api.statistics_module.model.autoresponder.AnswerSearchSummaryStatsDTO;
import com.example.domain.api.statistics_module.model.autoresponder.TextProcessingSummaryStatsDTO;
import com.example.domain.api.statistics_module.model.chat.ChatSummaryStatsDTO;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MetricSummaryDto {
    private AuthSummaryStatsDTO auth;
    private ChatSummaryStatsDTO chat;
    private RegistrationSummaryStatsDTO registration;
    private RoleSummaryStatsDTO role;
    private AnswerSearchSummaryStatsDTO answerSearch;
    private TextProcessingSummaryStatsDTO textProcessing;
}
