package com.aron.studio.data.dto.user;

import lombok.Data;

@Data
public class UpdatePasswordDTO {

    private String userId; // 登陆用户，代码要校验，每个用户只能更改自己的密码
    private String oldPassword;
    private String newPassword;
}
