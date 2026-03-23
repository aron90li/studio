package com.aron.studio.data.dto.user;

import lombok.Data;

@Data
public class UpdateEnabledDTO {

    private String userId;
    private Boolean enabled;
}
