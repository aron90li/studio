package com.aron.studio.data.dto.user;

import lombok.Data;

@Data
public class ResetPasswordDTO {
    private String userId; // 要更改的用户id，不一定是登陆的用户即admin
    private String newPassword;
}
