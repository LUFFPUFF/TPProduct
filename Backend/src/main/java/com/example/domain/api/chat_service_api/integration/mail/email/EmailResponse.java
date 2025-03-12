package com.example.domain.api.chat_service_api.integration.mail.email;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Date;

@Data
@AllArgsConstructor
public class EmailResponse {

    private String subject;
    private String from;
    private String content;
    private Date receivedDate;

    @Override
    public String toString() {
        return "EmailResponse{" +
                "subject='" + subject + '\'' +
                ", from='" + from + '\'' +
                ", content=" + content +
                ", receivedDate=" + receivedDate +
                '}';
    }
}
