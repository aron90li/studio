package com.aron.studio.data.dto.login;

import lombok.Data;

@Data
public class RegisterDTO {

    private String username;
    private String password;
    private String code;
    private String uuid;
}
