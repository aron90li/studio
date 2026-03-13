package com.aron.studio.data.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ProjectVO {
    private String projectId;
    private String projectName;
    private String projectIdentity;
    private String description;
    private String createUsername;
    private String createUserId;
    private String updateUsername;
    private String updateUserId;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
