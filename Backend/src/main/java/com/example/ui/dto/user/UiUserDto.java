package com.example.ui.dto.user;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UiUserDto {

    private Integer id;
    private String fullName;
    private String status;
    private String gender;
    private String profilePicture;
}
