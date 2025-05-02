package com.example.ui.dto.client;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UiClientDto {

    private Integer id;
    private String name;
    private String type;
    private String tag;
}
