package com.example.domain.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MemberDto {
    String fullName;
    String email;
}
