package com.example.domain.dto;

import lombok.Builder;
import lombok.Data;


import java.util.List;

@Builder
@Data
public class CompanyWithMembersDto {
    CompanyDto company;
    List<MemberDto> members;
}
