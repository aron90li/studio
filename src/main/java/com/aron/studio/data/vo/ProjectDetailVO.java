package com.aron.studio.data.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ProjectDetailVO {
    private String projectId;
    private String detailType;
    private String detailValue;
    private String createUsername;
    private String createUserId;
    private String updateUsername;
    private String updateUserId;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
