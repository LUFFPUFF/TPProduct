package com.example.domain.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;



@Data
public class CheckCodeDto {
    @NotNull
    @Size(min = 6, max = 6)
    private String code;
}
